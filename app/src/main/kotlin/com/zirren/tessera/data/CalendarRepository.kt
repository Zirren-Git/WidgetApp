package com.zirren.tessera.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import com.zirren.tessera.core.calendar.AttendanceStatus
import com.zirren.tessera.core.calendar.Availability
import com.zirren.tessera.core.calendar.CalendarInfo
import com.zirren.tessera.core.calendar.EventInstance
import java.time.Instant

/**
 * Reads the calendars already on the phone (Google, Exchange, DAVx⁵, local…) through
 * Android's CalendarContract. Read-only, and nothing leaves the device.
 */
class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun calendars(): List<CalendarInfo> {
        if (!hasPermission()) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
        )
        val order = "${CalendarContract.Calendars.ACCOUNT_NAME} ASC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        return try {
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, order)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            CalendarInfo(
                                id = cursor.getLong(0),
                                name = cursor.getString(1).orEmpty(),
                                accountName = cursor.getString(2),
                                color = opaque(cursor.getInt(3)),
                                isVisible = cursor.getInt(4) == 1,
                            ),
                        )
                    }
                }
            } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun instances(from: Instant, to: Instant): List<EventInstance> {
        if (!hasPermission()) return emptyList()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, from.toEpochMilli())
        ContentUris.appendId(builder, to.toEpochMilli())
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.AVAILABILITY,
        )
        val selection = "${CalendarContract.Instances.STATUS} IS NULL OR ${CalendarContract.Instances.STATUS} != ?"
        val args = arrayOf(CalendarContract.Events.STATUS_CANCELED.toString())
        return try {
            context.contentResolver.query(builder.build(), projection, selection, args, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            EventInstance(
                                eventId = cursor.getLong(0),
                                calendarId = cursor.getLong(1),
                                title = cursor.getString(2),
                                location = cursor.getString(3),
                                begin = Instant.ofEpochMilli(cursor.getLong(4)),
                                end = Instant.ofEpochMilli(cursor.getLong(5)),
                                allDay = cursor.getInt(6) == 1,
                                color = if (cursor.isNull(7)) null else opaque(cursor.getInt(7)),
                                attendance = attendance(cursor.getInt(8)),
                                availability = availability(cursor.getInt(9)),
                            ),
                        )
                    }
                }
            } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    private fun attendance(value: Int) = when (value) {
        CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED -> AttendanceStatus.ACCEPTED
        CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED -> AttendanceStatus.DECLINED
        CalendarContract.Attendees.ATTENDEE_STATUS_INVITED -> AttendanceStatus.INVITED
        CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE -> AttendanceStatus.TENTATIVE
        else -> AttendanceStatus.NONE
    }

    private fun availability(value: Int) = when (value) {
        CalendarContract.Events.AVAILABILITY_FREE -> Availability.FREE
        CalendarContract.Events.AVAILABILITY_TENTATIVE -> Availability.TENTATIVE
        else -> Availability.BUSY
    }

    /** Some calendar providers store colours without an alpha channel, which would draw as invisible. */
    private fun opaque(color: Int): Int = color or 0xFF000000.toInt()
}
