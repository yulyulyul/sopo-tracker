package team.sopo.common.util

import org.apache.commons.lang3.StringUtils
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TimeUtil {
    companion object {
        fun convert(text: String, formatter: DateTimeFormatter): String {
            if(StringUtils.isBlank(text)){
                return text
            }
            if(!checkTimeFormat(text, formatter)){
                return ""
            }
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
            if(StringUtils.isBlank(text)){
                return true
            }
            return try {
                ZonedDateTime.parse(text, formatter.withZone(ZoneId.of("Asia/Seoul")))
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}