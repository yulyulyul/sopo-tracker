package team.sopo.infrastructure.carrierselector

import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.exception.ParcelNotFoundException
import team.sopo.common.exception.ValidationException
import team.sopo.common.parcel.*
import team.sopo.common.util.ParcelUtil.Companion.determineState
import team.sopo.common.util.ParcelUtil.Companion.sorting
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand
import java.util.regex.Pattern
import kotlin.streams.toList

@Component
class ChunilpsSelector : CarrierSelector() {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.CHUNILPS.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        verifyWaybillNum(command.waybillNum)

        val document = Jsoup.connect("http://www.chunil.co.kr/HTrace/HTrace.jsp")
            .ignoreContentType(true)
            .data("transNo", command.waybillNum)
            .get()

        checkConvertable(document)

        return toParcel(document, command.carrierCode)
    }

    override fun calculateStatus(criteria: String): Status {
        if (StringUtils.isBlank(criteria)) {
            throw IllegalStateException("Status를 처리할 수 없습니다.(Chunlips)")
        }
        val processedCriteria = criteria.trim().replace(" ", "")
        return if (processedCriteria.contains("접수")) {
            Status.getInformationReceived()
        } else if (processedCriteria.contains("발송")) {
            Status.getAtPickUp()
        } else if (processedCriteria.contains("배송완료")) {
            Status.getDelivered()
        } else {
            Status.getInTransit()
        }
    }

    private fun toParcel(document: Document, carrierCode: String): Parcel {
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(carrierCode))
        val elements = document.select("table[cellspacing='1']")

        parcel.from = From(elements[0].select("tbody > tr > td")[1].text())
        parcel.to = To(elements[1].select("tbody > tr > td")[1].text())
        parcel.item = elements[2].select("tbody > tr > td")[1].text()

        val progress = elements[4].select("tbody > tr")
        parcel.progresses.addAll(
            progress.stream()
                .filter { it != progress.first() }
                .map {
                    val detail = it.select("td")
                    Progresses(
                        time = detail[0].text(),
                        location = Location(detail[1].text()),
                        status = calculateStatus(detail[3].text()),
                        description = detail[3].text()
                    )
                }.toList()
        )
        parcel = sorting(parcel)
        parcel.state = determineState(parcel)

        return parcel
    }

    private fun verifyWaybillNum(waybillNum: String) {
        val pattern = Pattern.compile("^[0-9]*?")
        val isValidNum = waybillNum.length == 11 && pattern.matcher(waybillNum).matches()

        if (!isValidNum) {
            throw ValidationException("송장번호의 유효성을 확인해주세요. - ($waybillNum)")
        }
    }

    private fun checkConvertable(document: Document) {
        val progresses = document.select("table[cellspacing='1']")
        if (progresses.size == 0) {
            throw ParcelNotFoundException("해당 송장번호에 부합하는 택배를 찾을 수 없습니다.")
        }
    }
}