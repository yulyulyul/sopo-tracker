package team.sopo.infrastructure.carrierselector

import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.exception.ParcelNotFoundException
import team.sopo.common.exception.ValidationException
import team.sopo.common.parcel.*
import team.sopo.common.util.ParcelUtil
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand
import team.sopo.infrastructure.carrierselector.cvsnet.GsResponse
import java.util.regex.Pattern
import kotlin.streams.toList

@Component
class CvsnetSelector : CarrierSelector() {

    private enum class CarrierType {
        GS_NETWORKS, DAEHAN_EXPRESS
    }

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.CVSNET.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        verifyWaybillNum(command.waybillNum)

        val document = Jsoup.connect("https://www.cvsnet.co.kr/invoice/tracking.do?invoice_no=${command.waybillNum}")
            .ignoreContentType(true)
            .get()

        checkConvertable(document)

        return toParcel(document, command.carrierCode)
    }

    override fun calculateStatus(criteria: String): Status {
        return Status("", "")
    }

    private fun toParcel(document: Document, carrierCode: String): Parcel {
        val summary = document.select("script")
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(carrierCode))
        val pattern = Pattern.compile(".*var trackingInfo = ([^;]*);")
        val matcher = pattern.matcher(summary[1].data())
        val gsRes = Gson().fromJson(matcher.group(1), GsResponse::class.java)

        parcel.from = From(gsRes.sender.name, null, gsRes.sender.tel)
        parcel.to = To(gsRes.receiver.name, null)
        parcel.item = gsRes.goodsName

        val progresses = gsRes.trackingDetails.stream().map { detail ->
            Progresses(
                time = detail.transTime,
                location = Location(detail.transWhere),
                status = calculateStatus(detail.transCode, gsRes.carrierType),
                description = detail.transKind
            )
        }.toList()
        parcel.progresses.addAll(progresses)
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)

        return parcel
    }

    private fun calculateStatus(criteria: String, carrierType: String): Status {
        if (StringUtils.isBlank(criteria)) {
            throw IllegalStateException("Status를 처리할 수 없습니다.($criteria)")
        }
        val processed = criteria.trim().replace(" ", "")

        return when (CarrierType.valueOf(carrierType)) {
            CarrierType.DAEHAN_EXPRESS -> daehanExpress(processed)
            CarrierType.GS_NETWORKS -> gsNetworks(processed)
        }
    }

    private fun gsNetworks(criteria: String): Status {
        return when (criteria) {
            "C01" -> Status.getInformationReceived()
            "C015" -> Status.getAtPickUp()
            "C10" -> Status.getDelivered()
            "C11" -> Status.getDelivered()
            else -> Status.getInTransit()
        }
    }

    private fun daehanExpress(criteria: String?): Status {
        return when (criteria) {
            null -> Status(Status.getInformationReceived().id, Status.getInformationReceived().text)
            "11" -> Status(State.getAtPickUp().id, State.getAtPickUp().text)
            "41" -> Status(State.getInTransit().id, State.getInTransit().text)
            "42" -> Status(State.getInTransit().id, State.getInTransit().text)
            "44" -> Status(State.getInTransit().id, State.getInTransit().text)
            "82" -> Status(State.getOutForDelivery().id, State.getOutForDelivery().text)
            "91" -> Status(State.getDelivered().id, State.getDelivered().text)
            else -> throw IllegalStateException("존재하지 않는 배송상태 입니다.(GS)")
        }
    }

    private fun verifyWaybillNum(waybillNum: String) {
        val pattern = Pattern.compile("^[0-9]*?")
        val isValidNum = waybillNum.length == 12 && pattern.matcher(waybillNum).matches()

        if (!isValidNum) {
            throw ValidationException("송장번호의 유효성을 확인해주세요. - ($waybillNum)")
        }
    }

    private fun checkConvertable(document: Document) {
        val summary = document.select("script")
        val pattern = Pattern.compile(".*var trackingInfo = ([^;]*);")
        val matcher = pattern.matcher(summary[1].data())
        if (!matcher.find()) {
            throw IllegalStateException("파싱 로직에 문제가 있습니다.")
        }
        val gsRes = Gson().fromJson(matcher.group(1), GsResponse::class.java)

        if (gsRes.code != 200) {
            throw ParcelNotFoundException("해당 송장번호에 부합하는 택배를 찾을 수 없습니다.")
        }
    }

}