package team.sopo.infrastructure.carrierselector

import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.exception.ParcelNotFoundException
import team.sopo.common.exception.ValidationException
import team.sopo.common.parcel.*
import team.sopo.common.util.ParcelUtil
import team.sopo.common.util.TimeUtil
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand
import team.sopo.infrastructure.carrierselector.hdexp.HdResponse
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.streams.toList

@Component
class HdexpSelector : CarrierSelector() {

    companion object {
        private val summaryFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val progressFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.s")
    }

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.HDEXP.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {

        verifyWaybillNum(command.waybillNum)

        val document = Jsoup.connect("https://hdexp.co.kr/deliverySearch2.hd")
            .ignoreContentType(true)
            .data("barcode", command.waybillNum)
            .get()
        val body = document.body().text()
        val hdRes = Gson().fromJson(body, HdResponse::class.java)

        checkConvertable(hdRes)

        return toParcel(hdRes)
    }

    override fun calculateStatus(criteria: String): Status {
        return when (criteria) {
            "접수완료" -> Status.getInformationReceived()
            "영업소집하" -> Status.getAtPickUp()
            "배송완료" -> Status.getDelivered()
            else -> Status.getInTransit()
        }
    }

    fun toParcel(hdRes: HdResponse): Parcel {
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(SupportCarrier.HDEXP.code))

        parcel.from = From(hdRes.info.send_name, TimeUtil.convert(hdRes.info.pd_dt, summaryFormatter), null)
        parcel.to = To(hdRes.info.re_name, TimeUtil.convert(hdRes.info.rec_dt, summaryFormatter))
        parcel.item = hdRes.info.prod

        val progresses = hdRes.items.stream()
            .filter { TimeUtil.checkTimeFormat(it.reg_date, progressFormatter) }
            .map { item ->
                Progresses(
                    time = TimeUtil.convert(item.reg_date, progressFormatter),
                    location = Location(item.location),
                    status = calculateStatus(item.stat),
                    description = item.stat
                )
            }.toList()
        parcel.progresses.addAll(progresses)
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)

        return parcel
    }

    private fun verifyWaybillNum(waybillNum: String) {
        val pattern = Pattern.compile("^[0-9]*?")
        val isValidNum = pattern.matcher(waybillNum).matches()

        if (!isValidNum) {
            throw ValidationException("송장번호의 유효성을 확인해주세요. - ($waybillNum)")
        }
    }

    private fun checkConvertable(response: HdResponse) {
        if (response.result == HdResponse.FAIL) {
            throw ParcelNotFoundException("해당 송장번호에 부합하는 택배를 찾을 수 없습니다.")
        }
    }
}