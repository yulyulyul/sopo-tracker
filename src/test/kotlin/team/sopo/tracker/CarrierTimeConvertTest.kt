package team.sopo.tracker

import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CarrierTimeConvertTest {
    @Test
    fun lotteTime() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val parse = ZonedDateTime.parse("2021-05-25 09:24:01", formatter.withZone(ZoneId.of("Asia/Seoul")))

        println(toStringTest(parse))
    }

    private fun toStringTest(zonedDateTime: ZonedDateTime): String {
        val dateTime = zonedDateTime.toLocalDateTime()
        val offset = zonedDateTime.offset
        return if (dateTime.second == 0) {
            "${dateTime}:00"
        } else {
            dateTime.toString()
        } + offset.toString()
    }

}