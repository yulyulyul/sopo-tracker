package team.sopo.common.util

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TimeUtil {
    companion object {
        fun convert(text: String, formatter: DateTimeFormatter): String {
            val zonedDateTime = ZonedDateTime.parse(text, formatter.withZone(ZoneId.of("Asia/Seoul")))
            val dateTime = zonedDateTime.toLocalDateTime()
            val offset = zonedDateTime.offset
            return if (dateTime.second == 0) {
                "${dateTime}:00"
            } else {
                dateTime.toString()
            } + offset.toString()
        }

        fun checkTimeFormat(text: String, formatter: DateTimeFormatter): Boolean {
            return try {
                convert(text, formatter)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}