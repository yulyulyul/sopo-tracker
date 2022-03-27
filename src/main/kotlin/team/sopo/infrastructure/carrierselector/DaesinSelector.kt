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
class DaesinSelector : CarrierSelector {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.DAESIN.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        val document = Jsoup.connect("https://www.ds3211.co.kr/freight/internalFreightSearch.ht")
            .ignoreContentType(true)
            .data("billno", command.waybillNum)
            .get()

        return toParcel(document, command.carrierCode)
    }

    private fun toParcel(document: Document, carrierCode: String): Parcel {
        val elements = document.select("table > tbody")
        val summary = elements[0].select("tr > td")
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(carrierCode))

        parcel.from = From(summary[0].text(), null, summary[1].text())
        parcel.to = To(summary[2].text())
        parcel.item = summary[4].text()

        val details = elements[1].select("tr")
        val progresses = details.stream()
            .filter { it != details.first() }
            .map { detail ->
                val data = detail.select("td")
                Progresses(
                    time = data[3].text(),
                    location = Location(data[0].text()),
                    status = calculateStatus(data[5].text()),
                    description = data[1].text()
                )
            }.toList()
        parcel.progresses.addAll(progresses)
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
        val processed = criteria.trim().replace(" ", "")
        return if (processed.contains("배송완료")) {
            Status.getDelivered()
        } else {
            Status.getInTransit()
        }
    }

}