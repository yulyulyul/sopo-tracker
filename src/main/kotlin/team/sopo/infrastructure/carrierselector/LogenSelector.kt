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
import java.util.regex.Pattern
import kotlin.streams.toList

@Component
class LogenSelector : CarrierSelector() {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.LOGEN.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        verifyWaybillNum(command.waybillNum)

        val document = Jsoup.connect("https://www.ilogen.com/web/personal/trace/${command.waybillNum}")
            .ignoreContentType(true)
            .get()

        checkConvertable(document)

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

        parcel.item = summary[0].select("td")[3].text()
        parcel.from = From(summary[3].select("td")[1].text(), null, null)
        parcel.to = To(summary[3].select("td")[3].text(), null)

        atPickUp.time = summary[1].select("td")[1].text()
        atPickUp.location = Location(summary[2].select("td")[1].text())
        parcel.progresses.add(atPickUp)

        val progresses = progress.stream().map { detail ->
            val elements = detail.select("td")
            Progresses(
                time = elements[0].text(),
                location = Location(elements[1].text()),
                status = calculateStatus(elements[2].text()),
                description = elements[3].text()
            )
        }.toList()
        parcel.progresses.addAll(progresses)
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)

        return parcel
    }

    private fun verifyWaybillNum(waybillNum: String){
        val num = waybillNum.replace("-", "")
        val pattern = Pattern.compile("^[0-9]*?")

        val isValidNum = num.length == 11 && pattern.matcher(num).matches()
        if(!isValidNum){
            throw ValidationException("송장번호의 유효성을 확인해주세요. - ($waybillNum)")
        }
    }

    private fun checkConvertable(document: Document) {
        /**
         *  element.size => 1인 경우, 아직 등록된 택배가 없는 경우임.
         *
         *  1 이상인 경우, 일반적인 배송과정의 시작으로 판단함.
         *  '시스템 점검'인 경우, 해당 size가 어떻게 될지에 따라, convert가 가능할지 여부를 판단해야할듯.
         */

        val element = document.select("table[class='horizon pdInfo'] tbody > tr")
        if (element.size == 1) {
            throw ParcelNotFoundException("해당 송장번호에 부합하는 택배를 찾을 수 없습니다.")
        }
    }
}