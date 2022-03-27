package team.sopo.infrastructure.carrierselector

import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.parcel.*
import team.sopo.common.util.ParcelUtil
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand
import team.sopo.infrastructure.carrierselector.cjlogistics.CjResponse
import kotlin.streams.toList

@Component
class CJlogisticsSelector : CarrierSelector() {

    override fun support(carrierCode: String): Boolean {
        return StringUtils.equals(carrierCode, SupportCarrier.CJ_LOGISTICS.code)
    }

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        val cjRes1 = try {
            Jsoup.connect("https://www.cjlogistics.com/ko/tool/parcel/tracking").execute()
        } catch (e: HttpStatusException) {
            throw IllegalStateException("CJ와의 연결에 실패했습니다.")
        }

        val doc = cjRes1.parse()
        val csrf = doc.select("input[name=_csrf]")
            .first()
            ?.attr("value") ?: throw IllegalStateException("_csrf 값을 찾을 수 없습니다.")

        val cjRes2 = Jsoup.connect("https://www.cjlogistics.com/ko/tool/parcel/tracking-detail")
            .ignoreContentType(true)
            .cookies(cjRes1.cookies())
            .data("paramInvcNo", command.waybillNum)
            .data("_csrf", csrf)
            .post()
            .body()
            .text()

        val cjResponse = Gson().fromJson(cjRes2, CjResponse::class.java)

        return toParcel(cjResponse)
    }

    override fun calculateStatus(criteria: String): Status {
        return when (criteria) {
            "" -> Status.getInformationReceived()
            "11" -> Status.getAtPickUp()
            "41" -> Status.getInTransit()
            "42" -> Status.getInTransit()
            "44" -> Status.getInTransit()
            "82" -> Status.getOutForDelivery()
            "91" -> Status.getDelivered()
            else -> throw IllegalStateException("존재하지 않는 배송상태 입니다.(CJ)")
        }
    }

    fun toParcel(cjRes: CjResponse): Parcel {
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(SupportCarrier.CJ_LOGISTICS.code))
        val parcelResult = cjRes.parcelResultMap.resultList.firstOrNull()
            ?: throw IllegalStateException("Cj Response에서 ResultList를 추출할 수 없습니다.")

        parcel.from = From(name = parcelResult.sendrNm, time = null, tel = null)
        parcel.to = To(name = parcelResult.rcvrNm, null)
        parcel.item = parcelResult.itemNm

        val progresses = cjRes.parcelDetailResultMap.resultList.stream().map { detail ->
            Progresses(
                time = detail.dTime,
                location = Location(detail.regBranNm),
                status = calculateStatus(detail.crgSt),
                description = detail.crgNm
            )
        }.toList()
        parcel.progresses.addAll(progresses)
        parcel = ParcelUtil.sorting(parcel)
        parcel.state = ParcelUtil.determineState(parcel)

        return parcel
    }

}