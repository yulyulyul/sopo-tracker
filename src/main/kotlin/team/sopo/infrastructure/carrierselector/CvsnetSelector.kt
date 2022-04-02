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
import team.sopo.common.util.TimeUtil
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand
import team.sopo.infrastructure.carrierselector.cvsnet.GsResponse
import team.sopo.infrastructure.carrierselector.cvsnet.TrackingDetail
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.streams.toList

@Component
class CvsnetSelector : CarrierSelector() {

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }

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
        val regex = """.*var trackingInfo = ([^;]*);""".toRegex()
        val data = summary[1].data()
        val group = regex.find(data)!!.groups[1]!!.value
        val gsRes = Gson().fromJson(group, GsResponse::class.java)

        parcel.from = From(gsRes.sender.name, null, gsRes.sender.tel)
        parcel.to = To(gsRes.receiver.name, null)
        parcel.item = gsRes.goodsName
        parcel.progresses.addAll(toProgresses(gsRes))
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
            "82" -> Status(State.getOutForDelivery().id, State.getOutForDelivery().text)
            "91" -> Status(State.getDelivered().id, State.getDelivered().text)
            else -> Status(State.getInTransit().id, State.getInTransit().text)
        }
    }

    private fun toProgresses(gsRes: GsResponse): List<Progresses> {
        return gsRes.trackingDetails.stream()
            .filter { checkTimeFormat(it) }
            .map { detail ->
                Progresses(
                    time = TimeUtil.convert(detail.transTime, formatter),
                    location = Location(detail.transWhere),
                    status = calculateStatus(detail.transCode, gsRes.carrierType),
                    description = detail.transKind
                )
            }.toList()
    }

    private fun checkTimeFormat(detail: TrackingDetail): Boolean {
        return TimeUtil.checkTimeFormat(detail.transTime, formatter)
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
        val regex = """.*var trackingInfo = ([^;]*);""".toRegex()
        val trackingInfo = summary[1].data()
        val matchResult =
            regex.find(trackingInfo)?.groups?.get(1)?.value ?: throw IllegalStateException("파싱 로직에 문제가 있습니다.")
        val gsRes = Gson().fromJson(matchResult, GsResponse::class.java)

        if (gsRes.code != 200) {
            throw ParcelNotFoundException("해당 송장번호에 부합하는 택배를 찾을 수 없습니다.")
        }
    }

}