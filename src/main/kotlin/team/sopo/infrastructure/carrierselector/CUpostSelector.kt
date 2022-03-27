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
class CUpostSelector : CarrierSelector {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.CU_POST.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        val document = Jsoup.connect("https://www.cupost.co.kr/postbox/delivery/localResult.cupost")
            .ignoreContentType(true)
            .data("invoice_no", command.waybillNum)
            .get()

        return toParcel(document, command.carrierCode)
    }

    private fun toParcel(document: Document, carrierCode: String): Parcel {

        val elements = document.select("table[class='tableType1'] > tbody")
        val summary = elements[0].select("td")
        val progress = elements[2].select("tr")
        val targetStore = summary[7].text().trim()
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(carrierCode))

        parcel.from = From(summary[4].text(), "${summary[2].text()} ${summary[3].text()}")
        parcel.to = To(summary[5].text())
        parcel.item = summary[1].text()

        val progresses = progress.stream().map {
            val detail = it.select("td")
            Progresses(
                time = detail[0].text(),
                location = Location(detail[1].text()),
                status = calculateStatus(detail[2].text(), targetStore),
                description = detail[2].text()
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

    private fun calculateStatus(criteria: String, targetStore: String): Status {
        if (StringUtils.isBlank(criteria)) {
            throw IllegalStateException("Status를 처리할 수 없습니다.(CU)")
        }
        val processedCriteria = criteria.trim().replace(" ", "")
        return if (processedCriteria.contains("접수")) {
            Status.getInformationReceived()
        } else if (processedCriteria.contains("수거")) {
            Status.getAtPickUp()
        } else if (processedCriteria.contains(targetStore) && processedCriteria.contains("출고")) {
            Status.getOutForDelivery()
        } else if (processedCriteria.contains("도착") || processedCriteria.contains("수령")) {
            Status.getDelivered()
        } else {
            Status.getInTransit()
        }
    }
}