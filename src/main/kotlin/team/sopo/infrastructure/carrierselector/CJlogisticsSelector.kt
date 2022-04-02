package team.sopo.infrastructure.carrierselector

import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.jsoup.HttpStatusException
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
import team.sopo.infrastructure.carrierselector.cjlogistics.CjResponse
import team.sopo.infrastructure.carrierselector.cjlogistics.ParcelDetailResult
import java.time.format.DateTimeFormatter
import kotlin.streams.toList

@Component
class CJlogisticsSelector : CarrierSelector() {

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")
    }

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.CJ_LOGISTICS.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        verifyWaybillNum(command.waybillNum)

        val cjRes1 = try {
            Jsoup.connect("https://www.cjlogistics.com/ko/tool/parcel/tracking").execute()
        } catch (e: HttpStatusException) {
            throw IllegalStateException("CJ와의 연결에 실패했습니다.")
        }

        val doc = cjRes1.parse()
        val csrf = doc.select("input[name=_csrf]")
            .first()
            ?.attr("value") ?: throw IllegalStateException("_csrf 값을 찾을 수 없습니다.")

        val cjRes2 = Jsoup.connect("https://www.cjlogistics.com/ko/tool/parcel/tracking-detail")
            .ignoreContentType(true)
            .cookies(cjRes1.cookies())
            .data("paramInvcNo", command.waybillNum)
            .data("_csrf", csrf)
            .post()
            .body()
            .text()

        val cjResponse = Gson().fromJson(cjRes2, CjResponse::class.java)
        checkConvertable(cjResponse)

        return toParcel(cjResponse)
    }

    override fun calculateStatus(criteria: String): Status {
        return when (criteria) {
            "" -> Status.getInformationReceived()
            "11" -> Status.getAtPickUp()
            "82" -> Status.getOutForDelivery()
            "91" -> Status.getDelivered()
            else -> Status.getInTransit()
        }
    }

    fun toParcel(cjRes: CjResponse): Parcel {
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(SupportCarrier.CJ_LOGISTICS.code))
        val parcelResult = cjRes.parcelResultMap.resultList.firstOrNull()
            ?: throw IllegalStateException("Cj Response에서 ResultList를 추출할 수 없습니다.")

        parcel.from = From(name = parcelResult.sendrNm, time = null, tel = null)
        parcel.to = To(name = parcelResult.rcvrNm, null)
        parcel.item = parcelResult.itemNm
        parcel.progresses.addAll(toProgresses(cjRes))
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)

        return parcel
    }

    private fun toProgresses(cjRes: CjResponse): List<Progresses> {
        return cjRes.parcelDetailResultMap.resultList.stream()
            .filter { checkTimeFormat(it) }
            .map { detail ->
                Progresses(
                    time = TimeUtil.convert(detail.dTime, formatter),
                    location = Location(detail.regBranNm),
                    status = calculateStatus(detail.crgSt),
                    description = detail.crgNm
                )
            }.toList()
    }

    private fun checkTimeFormat(detail: ParcelDetailResult): Boolean {
        return TimeUtil.checkTimeFormat(detail.dTime, formatter)
    }

    private fun verifyWaybillNum(waybillNum: String) {
        val isValidNum = waybillNum.length == 10 || waybillNum.length == 12
        if (!isValidNum) {
            throw ValidationException("송장번호의 유효성을 확인해주세요. - ($waybillNum)")
        }
    }

    private fun checkConvertable(response: CjResponse) {
        if (response.parcelResultMap.resultList.isEmpty()) {
            throw ParcelNotFoundException("해당 송장번호에 부합하는 택배를 찾을 수 없습니다.")
        }
    }
}