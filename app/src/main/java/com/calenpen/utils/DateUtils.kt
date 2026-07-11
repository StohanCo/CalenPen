package com.calenpen.utils

import java.text.SimpleDateFormat
import java.util.*

/** Shared date-formatting helpers used throughout the app. */
object DateUtils {

    /** "YYYY-MM-DD" format used as a database key. */
    private val KEY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
    }

    /** "YYYY-MM" format used to group notes by month. */
    private val MONTH_KEY_FORMAT = SimpleDateFormat("yyyy-MM", Locale.US).apply {
        isLenient = false
    }

    /** Returns today's date as a "YYYY-MM-DD" string. */
    fun todayKey(): String = KEY_FORMAT.format(Date())

    /** Returns the current month as a "YYYY-MM" string. */
    fun currentMonthKey(): String = MONTH_KEY_FORMAT.format(Date())

    /** Formats a [Date] as "YYYY-MM-DD". */
    fun toDateKey(date: Date): String = KEY_FORMAT.format(date)

    /** Parses a "YYYY-MM-DD" string into a [Date], or null on failure. */
    fun parseDate(dateKey: String): Date? = try {
        KEY_FORMAT.parse(dateKey)
    } catch (e: Exception) {
        null
    }

    /** Returns a [Calendar] for the given "YYYY-MM-DD" key, or null on failure. */
    fun toCalendar(dateKey: String): Calendar? {
        val date = parseDate(dateKey) ?: return null
        return Calendar.getInstance().apply { time = date }
    }

    /**
     * Returns the "YYYY-MM" month prefix for a given "YYYY-MM-DD" key.
     * E.g. "2024-03-15" → "2024-03".
     */
    fun monthKeyOf(dateKey: String): String = dateKey.take(7)

    /** Returns the year-month string for the given [yearMonth] offset from today.
     *  offset = 0  → current month
     *  offset = -1 → previous month
     *  offset = 1  → next month
     */
    fun monthKeyWithOffset(offset: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        return MONTH_KEY_FORMAT.format(cal.time)
    }

    /**
     * Returns the first day of the month (1-based day-of-week: 1=Sunday … 7=Saturday)
     * for the given "YYYY-MM" month key.
     */
    fun firstDayOfWeekInMonth(monthKey: String): Int {
        val cal = Calendar.getInstance()
        val parts = monthKey.split("-")
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, 1)
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    /** Returns the number of days in the given "YYYY-MM" month. */
    fun daysInMonth(monthKey: String): Int {
        val cal = Calendar.getInstance()
        val parts = monthKey.split("-")
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** Builds a "YYYY-MM-DD" key from year, month (1-based), and day. */
    fun buildDateKey(year: Int, month: Int, day: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    /** Returns a user-friendly short label, e.g. "Mon, 15 Jan" for a date key. */
    fun toShortLabel(dateKey: String): String {
        val date = parseDate(dateKey) ?: return dateKey
        return SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(date)
    }

    /** Returns a user-friendly month + year label, e.g. "January 2024". */
    fun toMonthYearLabel(monthKey: String): String {
        return try {
            val date = MONTH_KEY_FORMAT.parse(monthKey) ?: return monthKey
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            monthKey
        }
    }
}
