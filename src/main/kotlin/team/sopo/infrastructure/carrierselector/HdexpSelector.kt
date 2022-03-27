package team.sopo.infrastructure.carrierselector

import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.parcel.*
import team.sopo.common.util.ParcelUtil
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand
import team.sopo.infrastructure.carrierselector.hdexp.HdResponse
import kotlin.streams.toList

@Component
class HdexpSelector : CarrierSelector() {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.HDEXP.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        val document = Jsoup.connect("https://hdexp.co.kr/deliverySearch2.hd")
            .ignoreContentType(true)
            .data("barcode", command.waybillNum)
            .get()
        val body = document.body().text()
        val hdRes = Gson().fromJson(body, HdResponse::class.java)

        return toParcel(hdRes)
    }

    override fun calculateStatus(criteria: String): Status {
        return when (criteria) {
            "접수완료" -> Status.getInformationReceived()
            "영업소집하" -> Status.getAtPickUp()
            "배송완료" -> Status.getDelivered()
            else -> Status.getInTransit()
        }
    }

    fun toParcel(hdRes: HdResponse): Parcel {
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(SupportCarrier.HDEXP.code))

        parcel.from = From(hdRes.info.send_name, hdRes.info.pd_dt, null)
        parcel.to = To(hdRes.info.re_name, hdRes.info.rec_dt)
        parcel.item = hdRes.info.prod

        val progresses = hdRes.items.stream().map { item ->
            Progresses(
                time = item.reg_date,
                location = Location(item.location),
                status = calculateStatus(item.stat),
                description = item.stat
            )
        }.toList()
        parcel.progresses.addAll(progresses)
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)

        return parcel
    }
}