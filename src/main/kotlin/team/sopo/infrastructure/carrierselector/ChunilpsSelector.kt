package team.sopo.infrastructure.carrierselector

import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.extension.removeSpecialCharacter
import team.sopo.common.extension.sortProgress
import team.sopo.common.parcel.*
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand
import kotlin.streams.toList

@Component
class ChunilpsSelector : CarrierSelector {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.CHUNILPS.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        val document = Jsoup.connect("http://www.chunil.co.kr/HTrace/HTrace.jsp")
            .ignoreContentType(true)
            .data("transNo", command.waybillNum)
            .get()

        return toParcel(document, command.carrierCode)
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
        parcel.state = calculateState(parcel)

        return parcel
    }

    private fun sorting(parcel: Parcel): Parcel {
        return parcel.apply {
            parcel.removeSpecialCharacter()
            parcel.sortProgress()
        }
    }

    private fun calculateState(parcel: Parcel): State {
        val status = parcel.progresses.lastOrNull()?.status ?: Status.getInformationReceived()
        return State(status.id, status.text)
    }

    private fun calculateStatus(criteria: String): Status {
        if (StringUtils.isBlank(criteria)) {
            throw IllegalStateException("Status를 처리할 수 없습니다.(Lotte)")
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
}