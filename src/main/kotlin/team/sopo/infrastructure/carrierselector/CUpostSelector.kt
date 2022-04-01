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
class CUpostSelector : CarrierSelector() {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.CU_POST.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        verifyWaybillNum(command.waybillNum)

        val document = Jsoup.connect("https://www.cupost.co.kr/postbox/delivery/localResult.cupost")
            .ignoreContentType(true)
            .data("invoice_no", command.waybillNum)
            .get()

        checkConvertable(document)

        return toParcel(document, command.carrierCode)
    }

    override fun calculateStatus(criteria: String): Status {
        if (StringUtils.isBlank(criteria)) {
            throw IllegalStateException("Status를 처리할 수 없습니다.(CU)")
        }
        val processedCriteria = criteria.trim().replace(" ", "")
        return if (processedCriteria.contains("접수")) {
            Status.getInformationReceived()
        } else if (processedCriteria.contains("수거")) {
            Status.getAtPickUp()
        } else if (processedCriteria.contains("출고")) {
            Status.getOutForDelivery()
        } else if (processedCriteria.contains("도착") || processedCriteria.contains("수령")) {
            Status.getDelivered()
        } else {
            Status.getInTransit()
        }
    }

    private fun toParcel(document: Document, carrierCode: String): Parcel {

        val elements = document.select("table[class='tableType1'] > tbody")
        val summary = elements[0].select("td")
        val progress = elements[2].select("tr")
//        val targetStore = summary[7].text().trim()
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(carrierCode))

        parcel.from = From(summary[4].text(), "${summary[2].text()} ${summary[3].text()}")
        parcel.to = To(summary[5].text())
        parcel.item = summary[1].text()

        val progresses = progress.stream().map {
            val detail = it.select("td")
            Progresses(
                time = detail[0].text(),
                location = Location(detail[1].text()),
                status = calculateStatus(detail[2].text()),
                description = detail[2].text()
            )
        }.toList()

        parcel.progresses.addAll(progresses)
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)

        return parcel
    }

    private fun verifyWaybillNum(waybillNum: String) {
        val pattern = Pattern.compile("^[0-9]*?")
        val isValidNum = waybillNum.length in 10..12 && pattern.matcher(waybillNum).matches()

        if (!isValidNum) {
            throw ValidationException("송장번호의 유효성을 확인해주세요. - ($waybillNum)")
        }
    }

    private fun checkConvertable(document: Document) {
        val progresses = document.select("table[class='tableType1'] > tbody")
        if (progresses.size == 1) {
            throw ParcelNotFoundException("해당 송장번호에 부합하는 택배를 찾을 수 없습니다.")
        }
    }
}