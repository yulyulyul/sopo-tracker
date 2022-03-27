package team.sopo.tracker

import com.google.gson.Gson
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import team.sopo.common.SupportCarrier
import team.sopo.common.parcel.*
import team.sopo.infrastructure.carrierselector.cvsnet.GsResponse
import team.sopo.infrastructure.carrierselector.hdexp.HdResponse
import java.util.regex.Pattern
import kotlin.streams.toList

class SopoTrackerApplicationTests {

    @Test
    fun 합동 () {
        val document = Jsoup.connect("https://hdexp.co.kr/deliverySearch2.hd")
            .ignoreContentType(true)
            .data("barcode", "3207220225160")
            .get()
        val body = document.body().text()
        val hdRes = Gson().fromJson(body, HdResponse::class.java)
        val parcel = hdRes.toParcel()
        println(parcel)
    }

    @Test
    fun 대신() {
        val document = Jsoup.connect("https://www.ds3211.co.kr/freight/internalFreightSearch.ht")
            .ignoreContentType(true)
            .data("billno", "8054101100865")
            .get()
        val elements = document.select("table > tbody")
        val summary = elements[0].select("tr > td")
        var parcel = Parcel(carrier = SupportCarrier.toCarrier("kr.daesin"))

        parcel.from = From(summary[0].data(), null, summary[1].data())
        parcel.to = To(summary[2].data())
        parcel.item = summary[4].data()

        val details = elements[1].select("tr")
        val progresses = details.stream()
            .filter { it != details.first() }
            .map { detail ->
                val data = detail.select("td")
                Progresses(
                    time = data[3].data(),
                    location = Location(data[0].data()),
                    status = Status("",""),
                    description = data[1].data()
                )
            }.toList()

        parcel.progresses.addAll(progresses)
    }

    @Test
    fun gs() {
        val document = Jsoup.connect("https://www.cvsnet.co.kr/invoice/tracking.do?invoice_no=210083503114")
            .ignoreContentType(true)
            .get()

        val select = document.select("script")
        println(select)
        val pattern = Pattern.compile(".*var trackingInfo = ([^;]*);")
        val matcher = pattern.matcher(select[1].data())
        if (matcher.find()) {
            val response = matcher.group(1)
            val gsRes = Gson().fromJson(response, GsResponse::class.java)
        }
    }

    @Test
    fun cu() {
        val document = Jsoup.connect("https://www.cupost.co.kr/postbox/delivery/localResult.cupost")
            .ignoreContentType(true)
            .data("invoice_no", "22032527193")
            .get()

        val elements = document.select("table[class='tableType1'] > tbody")
        val summary = elements[0].select("td")
        val progress = elements[2].select("tr")
        val parcel = Parcel(carrier = SupportCarrier.toCarrier("kr.cupost"))
        val targetStore = summary[7].text().trim()

        parcel.from = From(summary[4].text(), "${summary[2].text()} ${summary[3].text()}")
        parcel.to = To(summary[5].text())
        parcel.item = summary[1].text()

        val progresses = progress.stream().map {
            val detail = it.select("td")
            Progresses(detail[0].text(), Location(detail[1].text()), Status("", ""), detail[2].text())
        }.toList()
        parcel.progresses.addAll(progresses)

        println(parcel)
    }

    @Test
    fun 천일() {
        val document = Jsoup.connect("http://www.chunil.co.kr/HTrace/HTrace.jsp")
            .ignoreContentType(true)
            .data("transNo", "12189760724")
            .get()

        val parcel = Parcel(carrier = SupportCarrier.toCarrier("kr.chunilps"))
        val elements = document.select("table[cellspacing='1']")
        parcel.from = From(elements[0].select("tbody > tr > td")[1].text())
        parcel.to = To(elements[1].select("tbody > tr > td")[1].text())
        parcel.item = elements[2].select("tbody > tr > td")[1].text()

        val progress = elements[4].select("tbody > tr")
        parcel.progresses.addAll(
            progress.stream()
                .filter { it != progress.first() }
                .map {
                    val detail = it.select("td")
                    Progresses(
                        detail[0].text(),
                        Location(detail[1].text()),
                        Status(detail[3].text(), detail[3].text()),
                        detail[3].text()
                    )
                }.toList()
        )

        println(parcel)
    }

    @Test
    fun 한진() {
        val document =
            Jsoup.connect("http://www.hanjinexpress.hanjin.net/customer/hddcw18.tracking").ignoreContentType(true)
                .data("w_num", "421732247403").get()

        val summary = document.select("table[class='board-list-table delivery-tbl'] tbody > tr")
        val progresses = document.select("table[class='board-list-table'] tbody > tr")

        println(summary)
        println(progresses)

        val parcel = Parcel(carrier = SupportCarrier.toCarrier("kr.hanjin"))
        val elements = summary.select("td")
        println(elements)

        parcel.item = elements[0].text()
        parcel.from = From(elements[1].text())
        parcel.to = To(elements[2].text())

        val select = progresses.first()!!.select("td")
        println(select)

        val map = progresses.stream().map {
            val detail = it.select("td")
            Progresses(
                time = "${detail[0].text()}${detail[1].text()}",
                location = Location(detail[2].text()),
                status = Status("", ""),
                description = detail[3].text()
            )
        }.toList()

        parcel.progresses.addAll(map)
        println(parcel)
    }

    @Test
    fun 로젠() {
        val document =
            Jsoup.connect("https://www.ilogen.com/web/personal/trace/96633431683").ignoreContentType(true).get()

        val atPickUpProgress = document.select("table[class='horizon pdInfo'] tbody > tr")
        val progress = document.select("table[class='data tkInfo'] tbody > tr")

        val parcel = Parcel(carrier = SupportCarrier.toCarrier("kr.logen"))
        val atPickUp = Progresses(null, null, Status.getAtPickUp(), null)

        for (i in 0 until atPickUpProgress.size) {
            val elements = atPickUpProgress[i].select("td")
            when (i) {
                0 -> {
                    parcel.item = elements[3].text()
                }
                1 -> {
                    atPickUp.time = elements[1].text()
                }
                2 -> {
                    atPickUp.location = Location(elements[1].text())
                }
                3 -> {
                    parcel.from = From(elements[1].text(), null, null)
                    parcel.to = To(elements[3].text(), null)
                }
            }
        }
        parcel.progresses.add(atPickUp)

        for (i in 0 until progress.size) {
            val elements = progress[i].select("td")
            println(elements)
            parcel.progresses.add(
                Progresses(
                    time = elements[0].text(),
                    location = Location(elements[1].text()),
                    status = Status(elements[2].text(), elements[2].text()),
                    description = elements[3].text()
                )
            )
        }
        println(parcel)
    }

    @Test
    fun 우체국() {
        val document =
            Jsoup.connect("https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm").ignoreContentType(true)
                .data("sid1", "6077376185961").post()
//		println(document)

        val epostProgress = document.select("tbody > tr")
        val trackingInfo = Parcel(carrier = Carrier("kr.epost", "우체국 택배", "+8215881300"))
        for (i in 0 until epostProgress.size) {
            val elements = epostProgress[i].select("tr > td")
            if (i == 0) {
                trackingInfo.from = From(elements[0].childNode(0).toString(), elements[0].childNode(2).toString(), null)
                trackingInfo.to = To(elements[2].childNode(0).toString(), elements[2].childNode(2).toString())
                trackingInfo.state = State("delivered", elements[4].text())
            } else {
                trackingInfo.progresses.add(
                    Progresses(
                        location = Location(elements[2].text()),
                        status = Status(elements[3].text(), elements[3].text()),
                        time = "${elements[0].text()}${elements[1].text()}",
                        description = elements[3].text()
                    )
                )
            }
        }
        println(trackingInfo)
    }

    @Test
    fun 롯데() {
        val document =
            Jsoup.connect("https://www.lotteglogis.com/home/reservation/tracking/linkView").ignoreContentType(true)
                .data("InvNo", "241336723821").post()

        val lotteProgress = document.select("tbody > tr")
        val trackingInfo = Parcel(carrier = Carrier("kr.lotte", "롯데택배", "+8215882121"))
        for (i in 0 until lotteProgress.size) {
            val elements = lotteProgress[i].select("tr > td")
            if (i == 0) {
                trackingInfo.from = From(elements[1].text(), null, null)
                trackingInfo.to = To(elements[2].text(), null)
                trackingInfo.state = State("delivered", elements[3].text())
            } else {
                trackingInfo.progresses.add(
                    Progresses(
                        location = Location(elements[2].text()),
                        status = Status(elements[0].text(), elements[0].text()),
                        time = elements[1].text(),
                        description = elements[3].text()
                    )
                )
            }
        }

        println(trackingInfo)
    }

    @Test
    fun 대한통운() {
        val cjRes1 = Jsoup.connect("https://www.cjlogistics.com/ko/tool/parcel/tracking").execute()
        val doc = cjRes1.parse()
        val csrf =
            doc.select("input[name=_csrf]").first()?.attr("value") ?: throw IllegalStateException("_csrf 값을 찾을 수 없습니다.")
        val cjRes2 = Jsoup.connect("https://www.cjlogistics.com/ko/tool/parcel/tracking-detail").ignoreContentType(true)
            .cookies(cjRes1.cookies()).data("paramInvcNo", "647162973775").data("_csrf", csrf).post().body().text()
        println(cjRes2)
    }

}
