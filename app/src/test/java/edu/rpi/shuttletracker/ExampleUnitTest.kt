package edu.rpi.shuttletracker

import org.junit.Test

import org.junit.Assert.*
import org.junit.jupiter.api.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
//class ExampleUnitTest {
//    @Test
//    fun addition_isCorrect() {
//        assertEquals(4, 2 + 2)
//    }
//}
class ScheduleAPITest {
    @Test
    fun scheduleAPI_IsCorrect() {

        val testsched = scheduleAPI("https://shuttletracker.app/schedule.json")[0]

    }


}
