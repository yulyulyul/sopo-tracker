package team.sopo.infrastructure.carrierselector

import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.exception.ParcelNotFoundException
import team.sopo.common.exception.ValidationException
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
        verifyWaybillNum(command.waybillNum)

        val document = Jsoup.connect("https://www.lotteglogis.com/home/reservation/tracking/linkView")
            .ignoreContentType(true)
            .data("InvNo", command.waybillNum)
            .post()

        checkConvertable(document)

        return toParcel(document, command.carrierCode)
    }

    override fun calculateStatus(criteria: String): Status {
        if (StringUtils.isBlank(criteria)) {
            throw IllegalStateException("Status를 처리할 수 없습니다.($criteria)")
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

    private fun toParcel(document: Document, carrierCode: String): Parcel {
        val element = document.select("tbody > tr")
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

    private fun verifyWaybillNum(waybillNum: String) {
        val isValidNum = waybillNum.length in 9..12
        if (!isValidNum) {
            throw ValidationException("송장번호의 유효성을 확인해주세요. - ($waybillNum)")
        }
    }

    private fun checkConvertable(document: Document) {
        /**
         *  element.size => 2인 경우, 아직 등록된 택배가 없는 경우임.
         *
         *  2 이상인 경우, 일반적인 배송과정의 시작으로 판단함.
         *  '시스템 점검'인 경우, 해당 size가 어떻게 될지에 따라, convert가 가능할지 여부를 판단해야할듯.
         */

        val element = document.select("tbody > tr")

        if (element.size == 2) {
            throw ParcelNotFoundException("해당 송장번호에 부합하는 택배를 찾을 수 없습니다.")
        }
    }
}