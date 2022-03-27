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
import kotlin.streams.toList

@Component
class HanjinSelector : CarrierSelector() {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.HANJINS.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        val document = Jsoup.connect("http://www.hanjinexpress.hanjin.net/customer/hddcw18.tracking")
            .ignoreContentType(true)
            .data("w_num", command.waybillNum)
            .get()

        return toParcel(document, command.carrierCode)
    }

    override fun calculateStatus(criteria: String): Status {
        if (StringUtils.isBlank(criteria)) {
            throw IllegalStateException("Status를 처리할 수 없습니다.(EPost)")
        }
        val processedCriteria = criteria.trim().replace(" ", "")
        return if (processedCriteria.contains("집하") || processedCriteria.contains("접수")) {
            Status.getAtPickUp()
        } else if (processedCriteria.contains("배송출발")) {
            Status.getOutForDelivery()
        } else if (processedCriteria.contains("배송완료")) {
            Status.getDelivered()
        } else {
            Status.getInTransit()
        }
    }

    private fun toParcel(document: Document, carrierCode: String): Parcel {
        val summary = document.select("table[class='board-list-table delivery-tbl'] tbody > tr")
        val progresses = document.select("table[class='board-list-table'] tbody > tr")
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(carrierCode))
        val elements = summary.select("td")

        parcel.item = elements[0].text()
        parcel.from = From(elements[1].text())
        parcel.to = To(elements[2].text())
        parcel.progresses.addAll(
            progresses.stream().map {
                val detail = it.select("td")
                Progresses(
                    time = "${detail[0].text()} ${detail[1].text()}",
                    location = Location(detail[2].text()),
                    status = calculateStatus(detail[3].text()),
                    description = detail[3].text()
                )
            }.toList()
        )
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)

        return parcel
    }
}