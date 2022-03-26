package team.sopo.infrastructure.carrierselector

import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.extension.removeSpecialCharacter
import team.sopo.common.extension.sortProgress
import team.sopo.common.parcel.*
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand

@Component
class EPostSelector : CarrierSelector {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.EPOST.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        val document = Jsoup.connect("https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm")
            .ignoreContentType(true)
            .data("sid1", command.waybillNum)
            .post()
        val epostProgress = document.select("tbody > tr")

        return toParcel(epostProgress, command.carrierCode)
    }

    private fun toParcel(element: Elements, carrierCode: String): Parcel {
        val parcel = Parcel(carrier = SupportCarrier.toCarrier(carrierCode))
        for (i in 0 until element.size) {
            val elements = element[i].select("tr > td")
            if (i == 0) {
                val statePair = calculateState(elements[4].text())

                parcel.from = From(elements[0].childNode(0).toString(), elements[0].childNode(2).toString(), null)
                parcel.to = To(elements[2].childNode(0).toString(), elements[2].childNode(2).toString())
                parcel.state = State(statePair.first, statePair.second)
            } else {
                val statusPair = calculateState(elements[3].text())

                parcel.progresses.add(
                    Progresses(
                        location = Location(elements[2].text()),
                        status = Status(statusPair.first, statusPair.second),
                        time = "${elements[0].text()}${elements[1].text()}",
                        description = elements[3].text()
                    )
                )
            }
        }
        return parcel.apply {
            this.removeSpecialCharacter()
            this.sortProgress()
        }
    }

    private fun calculateState(criteria: String): Pair<String, String> {
        if (StringUtils.isBlank(criteria)) {
            throw IllegalStateException("Status를 처리할 수 없습니다.(EPost)")
        }
        val processedCriteria = criteria.trim().replace(" ", "")
        return if (processedCriteria.contains("접수")) {
            Pair(State.getAtPickUp().id, State.getAtPickUp().text)
        } else if (processedCriteria.contains("배달준비")) {
            Pair(State.getOutForDelivery().id, State.getOutForDelivery().text)
        } else if (processedCriteria.contains("배달완료")) {
            Pair(State.getDelivered().id, State.getDelivered().text)
        } else {
            Pair(State.getInTransit().id, State.getInTransit().text)
        }
    }
}