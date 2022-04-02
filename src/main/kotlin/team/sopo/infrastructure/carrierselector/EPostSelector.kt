package team.sopo.infrastructure.carrierselector

import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.exception.ParcelNotFoundException
import team.sopo.common.exception.ValidationException
import team.sopo.common.parcel.*
import team.sopo.common.util.ParcelUtil
import team.sopo.common.util.TimeUtil
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.streams.toList

@Component
class EpostSelector : CarrierSelector() {

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    }

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.EPOST.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        verifyWaybillNum(command.waybillNum)

        val document = Jsoup.connect("https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm")
            .ignoreContentType(true)
            .data("sid1", command.waybillNum)
            .post()

        checkConvertable(document)

        return toParcel(document, command.carrierCode)
    }

    override fun calculateStatus(criteria: String): Status {
        if (StringUtils.isBlank(criteria)) {
            throw IllegalStateException("Status를 처리할 수 없습니다.($criteria)")
        }
        val processedCriteria = criteria.trim().replace(" ", "")
        return if (processedCriteria.contains("접수")) {
            Status.getAtPickUp()
        } else if (processedCriteria.contains("배달준비")) {
            Status.getOutForDelivery()
        } else if (processedCriteria.contains("배달완료")) {
            Status.getDelivered()
        } else {
            Status.getInTransit()
        }
    }

    private fun toParcel(document: Document, carrierCode: String): Parcel {

        val elements = document.select("tbody > tr")
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(carrierCode))
        val summary = elements[0].select("tr > td")

        parcel.from = toFrom(summary)
        parcel.to = toTo(summary)
        parcel.progresses.addAll(toProgresses(elements))
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)

        return parcel
    }

    private fun toFrom(elements: Elements): From {
        val fromTime = elements[0].childNode(2).toString()
        return From(elements[0].childNode(0).toString(), TimeUtil.convert("$fromTime 00:00", formatter), null)
    }

    private fun toTo(elements: Elements): To {
        val toTime = elements[2].childNode(2).toString()
        return To(elements[2].childNode(0).toString(), TimeUtil.convert("$toTime 00:00", formatter))
    }

    private fun toProgresses(elements: Elements): List<Progresses> {
        return elements.stream()
            .filter { it != elements.first() }
            .filter { checkTimeFormat(it) }
            .map {
                toProgress(elements = it.select("tr > td"))
            }.toList()
    }

    private fun toProgress(elements: Elements): Progresses {
        val time = "${elements[0].text()} ${elements[1].text()}"
        return Progresses(
            location = Location(elements[2].text()),
            status = calculateStatus(elements[3].text()),
            time = TimeUtil.convert(time, formatter),
            description = elements[3].text()
        )
    }

    private fun checkTimeFormat(element: Element): Boolean {
        val data = element.select("tr > td")
        return TimeUtil.checkTimeFormat("${data[0].text()} ${data[1].text()}", formatter)
    }

    private fun verifyWaybillNum(waybillNum: String) {
        val pattern = Pattern.compile("^[0-9]*?")
        val isValidNum = waybillNum.length == 13 && pattern.matcher(waybillNum).matches()

        if (!isValidNum) {
            throw ValidationException("송장번호의 유효성을 확인해주세요. - ($waybillNum)")
        }
    }

    private fun checkConvertable(document: Document) {
        val progresses = document.select("tbody > tr")
        if (progresses.size == 2) {
            throw ParcelNotFoundException("해당 송장번호에 부합하는 택배를 찾을 수 없습니다.")
        }
    }

}