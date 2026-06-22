package vn.edu.usth.taskmanagement.service

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import vn.edu.usth.taskmanagement.domain.model.TaskModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Data class for events read FROM Google Calendar
data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean
) {
    fun getStartTimeFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        return sdf.format(startMillis)
    }
    fun getEndTimeFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        return sdf.format(endMillis)
    }
}

class CalendarSyncManager(private val context: Context) {

    fun hasCalendarPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    // ==========================================
    // WRITE: Push a task to Google Calendar
    // Returns the calendar event ID (for dedup), or null on failure
    // ==========================================
    fun syncTaskToCalendar(task: TaskModel): String? {
        if (!hasCalendarPermissions()) {
            Log.e("CalendarSync", "Missing calendar permissions")
            return null
        }

        if (task.dueDate == null) return null

        // Skip if already synced
        if (task.calendarEventId != null) {
            Log.d("CalendarSync", "Task '${task.title}' already synced (eventId=${task.calendarEventId})")
            return task.calendarEventId
        }

        val calendarId = getPrimaryCalendarId() ?: return null

        val startMillis = parseDateToMillis(task.dueDate)
        if (startMillis == -1L) return null

        // Default event duration: 1 hour
        val endMillis = startMillis + 60 * 60 * 1000

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, task.title)
            put(CalendarContract.Events.DESCRIPTION, task.description ?: "")
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment
            Log.d("CalendarSync", "Event inserted: $uri (eventId=$eventId)")
            eventId
        } catch (e: SecurityException) {
            Log.e("CalendarSync", "SecurityException when inserting event", e)
            null
        }
    }

    // ==========================================
    // READ: Pull events FROM Google Calendar for a specific date
    // ==========================================
    fun readEventsForDate(year: Int, month: Int, dayOfMonth: Int): List<CalendarEvent> {
        if (!hasCalendarPermissions()) return emptyList()

        val startCal = Calendar.getInstance().apply {
            set(year, month, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(year, month, dayOfMonth, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val events = mutableListOf<CalendarEvent>()

        try {
            // Use Instances API to query events for a date range (handles recurring events)
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startCal.timeInMillis)
            ContentUris.appendId(builder, endCal.timeInMillis)

            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            id = it.getLong(0),
                            title = it.getString(1) ?: "(No title)",
                            description = it.getString(2),
                            startMillis = it.getLong(3),
                            endMillis = it.getLong(4),
                            isAllDay = it.getInt(5) == 1
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e("CalendarSync", "SecurityException reading events", e)
        }

        Log.d("CalendarSync", "Read ${events.size} events for $year-${month+1}-$dayOfMonth")
        return events
    }

    // ==========================================
    // READ: Get unique dates containing Google Calendar events in a range
    // ==========================================
    fun readEventDatesInRange(startMillis: Long, endMillis: Long): List<Calendar> {
        if (!hasCalendarPermissions()) return emptyList()

        val projection = arrayOf(CalendarContract.Instances.BEGIN)
        val dates = mutableSetOf<String>() // Use "yyyy-MM-dd" to ensure uniqueness
        val calendars = mutableListOf<Calendar>()

        try {
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)

            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                null
            )

            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            cursor?.use {
                while (it.moveToNext()) {
                    val begin = it.getLong(0)
                    val cal = Calendar.getInstance().apply { timeInMillis = begin }
                    val dateStr = format.format(cal.time)
                    
                    if (dates.add(dateStr)) {
                        // Reset time to 00:00:00 to avoid duplicate EventDays in Applandeo Calendar
                        val pureDateCal = Calendar.getInstance().apply {
                            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        calendars.add(pureDateCal)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("CalendarSync", "SecurityException reading event dates", e)
        }

        return calendars
    }

    private fun getPrimaryCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )

            var bestId: Long? = null

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val isPrimary = it.getInt(1)
                    val accountType = it.getString(2)
                    
                    // Ưu tiên 1: Lịch chính (Primary) thuộc tài khoản Google
                    if (isPrimary == 1 && accountType == "com.google") {
                        return id
                    }
                    
                    // Ưu tiên 2: Bất kỳ lịch nào thuộc tài khoản Google
                    if (bestId == null && accountType == "com.google") {
                        bestId = id
                    }
                }
                
                // Ưu tiên 3: Bất kỳ lịch chính nào (kể cả không phải Google)
                if (bestId == null && it.moveToFirst()) {
                    do {
                        if (it.getInt(1) == 1) return it.getLong(0)
                    } while (it.moveToNext())
                }

                // Cuối cùng: Lấy lịch đầu tiên tìm thấy
                if (bestId == null && it.moveToFirst()) {
                    bestId = it.getLong(0)
                }
            }
            return bestId
        } catch (e: SecurityException) {
            Log.e("CalendarSync", "SecurityException reading calendars", e)
        }
        return null
    }

    fun parseDateToMillis(dateStr: String): Long {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                if (format.contains("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                } else {
                    sdf.timeZone = TimeZone.getDefault()
                }
                val date = sdf.parse(dateStr)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                // Try next format
            }
        }
        return -1L
    }
}

