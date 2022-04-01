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
class DaesinSelector : CarrierSelector() {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.DAESIN.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        verifyWaybillNum(command.waybillNum)

        val document = Jsoup.connect("https://www.ds3211.co.kr/freight/internalFreightSearch.ht")
            .ignoreContentType(true)
            .data("billno", command.waybillNum)
            .get()

        checkConvertable(document)

        return toParcel(document, command.carrierCode)
    }

    override fun calculateStatus(criteria: String): Status {
        val processed = criteria.trim().replace(" ", "")
        return if (processed.contains("배송완료")) {
            Status.getDelivered()
        } else {
            Status.getInTransit()
        }
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
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)

        return parcel
    }

    private fun verifyWaybillNum(waybillNum: String) {
        val num = waybillNum.replace("-", "")
        val pattern = Pattern.compile("^[0-9]*?")
        val isValidNum = num.length in 12..13 && pattern.matcher(num).matches()

        if (!isValidNum) {
            throw ValidationException("송장번호의 유효성을 확인해주세요. - ($waybillNum)")
        }
    }

    private fun checkConvertable(document: Document) {
        val progresses = document.select("table > tbody")
        if (progresses.size == 0) {
            throw ParcelNotFoundException("해당 송장번호에 부합하는 택배를 찾을 수 없습니다.")
        }
    }

}