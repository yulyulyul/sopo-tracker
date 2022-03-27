package team.sopo.infrastructure.carrierselector

import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.parcel.Parcel
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand
import team.sopo.infrastructure.carrierselector.kdexp.KdResponse

@Component
class KdexpSelector : CarrierSelector {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.KDEXP.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        val document = Jsoup.connect("https://kdexp.com/newDeliverySearch.kd")
            .ignoreContentType(true)
            .data("barcode", command.waybillNum)
            .get()
        val body = document.body().text()
        val hdRes = Gson().fromJson(body, KdResponse::class.java)

        return hdRes.toParcel()
    }
}