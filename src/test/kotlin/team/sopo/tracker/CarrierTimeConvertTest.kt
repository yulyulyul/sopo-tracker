package team.sopo.tracker

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import team.sopo.common.util.TimeUtil
import java.time.format.DateTimeFormatter

class CarrierTimeConvertTest {
    @Test
    fun lotteTime() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val time = "2021-05-25 09:24"

        Assertions.assertTrue(TimeUtil.checkTimeFormat(time, formatter))
    }

    @Test
    fun kdexpTime() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.s")
        val time = "2022-02-23 14:31:00.0"

        Assertions.assertTrue(TimeUtil.checkTimeFormat(time, formatter))
    }

}