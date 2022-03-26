package team.sopo.infrastructure.carrierselector

import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import team.sopo.common.SupportCarrier
import team.sopo.common.extension.removeSpecialCharacter
import team.sopo.common.extension.sortProgress
import team.sopo.common.parcel.*
import team.sopo.domain.tracker.CarrierSelector
import team.sopo.domain.tracker.TrackerCommand
import team.sopo.infrastructure.carrierselector.cjlogistics.CjResponse

@Component
class CJlogisticsSelector : CarrierSelector {

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

        return cjResponse.toParcel(command.carrierCode).apply {
            this.removeSpecialCharacter()
            this.sortProgress()
        }
    }
}