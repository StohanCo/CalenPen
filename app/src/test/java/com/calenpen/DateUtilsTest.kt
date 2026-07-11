package com.calenpen

import com.calenpen.utils.DateUtils
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class DateUtilsTest {

    @Test
    fun `todayKey returns YYYY-MM-DD formatted string`() {
        val key = DateUtils.todayKey()
        assertTrue("Key should match YYYY-MM-DD pattern", key.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun `buildDateKey formats correctly`() {
        assertEquals("2024-01-05", DateUtils.buildDateKey(2024, 1, 5))
        assertEquals("2024-12-31", DateUtils.buildDateKey(2024, 12, 31))
        assertEquals("2000-06-15", DateUtils.buildDateKey(2000, 6, 15))
    }

    @Test
    fun `monthKeyOf extracts year-month from date key`() {
        assertEquals("2024-03", DateUtils.monthKeyOf("2024-03-15"))
        assertEquals("2024-12", DateUtils.monthKeyOf("2024-12-01"))
    }

    @Test
    fun `parseDate returns non-null for valid key`() {
        val date = DateUtils.parseDate("2024-06-15")
        assertNotNull(date)
    }

    @Test
    fun `parseDate returns null for invalid key`() {
        val date = DateUtils.parseDate("not-a-date")
        assertNull(date)
    }

    @Test
    fun `toDateKey round-trips correctly`() {
        val original = "2024-07-04"
        val date = DateUtils.parseDate(original)!!
        val back = DateUtils.toDateKey(date)
        assertEquals(original, back)
    }

    @Test
    fun `daysInMonth returns 31 for January`() {
        assertEquals(31, DateUtils.daysInMonth("2024-01"))
    }

    @Test
    fun `daysInMonth returns 29 for February in leap year`() {
        assertEquals(29, DateUtils.daysInMonth("2024-02"))
    }

    @Test
    fun `daysInMonth returns 28 for February in non-leap year`() {
        assertEquals(28, DateUtils.daysInMonth("2023-02"))
    }

    @Test
    fun `daysInMonth returns 30 for April`() {
        assertEquals(30, DateUtils.daysInMonth("2024-04"))
    }

    @Test
    fun `toMonthYearLabel returns readable label`() {
        // Locale-independent: just verify it's non-blank and contains the year
        val label = DateUtils.toMonthYearLabel("2024-03")
        assertTrue(label.contains("2024"))
        assertTrue(label.isNotBlank())
    }

    @Test
    fun `monthKeyWithOffset zero returns current month`() {
        val current = DateUtils.currentMonthKey()
        val offset = DateUtils.monthKeyWithOffset(0)
        assertEquals(current, offset)
    }

    @Test
    fun `firstDayOfWeekInMonth returns value between 1 and 7`() {
        val dow = DateUtils.firstDayOfWeekInMonth("2024-01")
        assertTrue(dow in 1..7)
    }
}
