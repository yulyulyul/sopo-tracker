package team.sopo.infrastructure.carrierselector

import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.parcel.Parcel
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand
import team.sopo.infrastructure.carrierselector.hdexp.HdResponse

@Component
class HdexpSelector : CarrierSelector {

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

        return hdRes.toParcel()
    }
}