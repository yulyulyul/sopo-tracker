package team.sopo.infrastructure.carrierselector

import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.parcel.*
import team.sopo.common.util.ParcelUtil
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand

@Component
class LotteSelector : CarrierSelector() {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.LOTTE.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        val document = Jsoup.connect("https://www.lotteglogis.com/home/reservation/tracking/linkView")
            .ignoreContentType(true)
            .data("InvNo", command.waybillNum)
            .post()
        val lotteProgress = document.select("tbody > tr")

        return toParcel(lotteProgress, command.carrierCode)
    }

    override fun calculateStatus(criteria: String): Status {
        if (StringUtils.isBlank(criteria)) {
            throw IllegalStateException("Status를 처리할 수 없습니다.(Lotte)")
        }
        val processedCriteria = criteria.trim().replace(" ", "")
        return if (processedCriteria.contains("인수") || processedCriteria.contains("상품접수")) {
            Status.getAtPickUp()
        } else if (processedCriteria.contains("배송출발")) {
            Status.getOutForDelivery()
        } else if (processedCriteria.contains("배달완료")) {
            Status.getDelivered()
        } else {
            Status.getInTransit()
        }
    }

    private fun toParcel(element: Elements, carrierCode: String): Parcel {
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(carrierCode))
        for (i in 0 until element.size) {
            val elements = element[i].select("tr > td")
            if (i == 0) {
                parcel.from = From(elements[1].text())
                parcel.to = To(elements[2].text())
            } else {
                parcel.progresses.add(
                    Progresses(
                        location = Location(elements[2].text()),
                        status = calculateStatus(elements[0].text()),
                        time = elements[1].text(),
                        description = elements[3].text()
                    )
                )
            }
        }
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)
        return parcel
    }
}