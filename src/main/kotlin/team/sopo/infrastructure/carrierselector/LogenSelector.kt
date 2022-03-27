package team.sopo.infrastructure.carrierselector

import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.parcel.*
import team.sopo.common.util.ParcelUtil
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand

@Component
class LogenSelector : CarrierSelector() {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.LOGEN.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        val document = Jsoup.connect("https://www.ilogen.com/web/personal/trace/${command.waybillNum}")
            .ignoreContentType(true)
            .get()

        return toParcel(document, command.carrierCode)
    }

    override fun calculateStatus(criteria: String): Status {
        if (StringUtils.isBlank(criteria)) {
            throw IllegalStateException("Status를 처리할 수 없습니다.(Logen)")
        }
        val processedCriteria = criteria.trim().replace(" ", "")
        return if (processedCriteria.contains("배송출고")) {
            Status.getOutForDelivery()
        } else if (processedCriteria.contains("배송완료")) {
            Status.getDelivered()
        } else {
            Status.getInTransit()
        }
    }

    private fun toParcel(document: Document, carrierCode: String): Parcel {
        val summary = document.select("table[class='horizon pdInfo'] tbody > tr")
        val progress = document.select("table[class='data tkInfo'] tbody > tr")
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(carrierCode))
        val atPickUp = Progresses(status = Status.getAtPickUp())

        for (i in 0 until summary.size) {
            val elements = summary[i].select("td")
            when (i) {
                0 -> {
                    parcel.item = elements[3].text()
                }
                1 -> {
                    atPickUp.time = elements[1].text()
                }
                2 -> {
                    atPickUp.location = Location(elements[1].text())
                }
                3 -> {
                    parcel.from = From(elements[1].text())
                    parcel.to = To(elements[3].text())
                }
            }
        }
        parcel.progresses.add(atPickUp)

        for (i in 0 until progress.size) {
            val elements = progress[i].select("td")
            parcel.progresses.add(
                Progresses(
                    time = elements[0].text(),
                    location = Location(elements[1].text()),
                    status = calculateStatus(elements[2].text()),
                    description = elements[3].text()
                )
            )
        }
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)

        return parcel
    }
}