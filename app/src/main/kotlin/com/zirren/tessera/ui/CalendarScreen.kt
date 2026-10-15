package com.zirren.tessera.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.zirren.tessera.R
import com.zirren.tessera.core.calendar.CalendarInfo
import com.zirren.tessera.graph
import com.zirren.tessera.sync.Scheduler
import com.zirren.tessera.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CalendarScreen(navigator: Navigator, version: Int) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val graph = context.graph
    val settings = graph.settings
    var granted by remember { mutableStateOf(graph.calendar.hasPermission()) }
    var denied by remember { mutableStateOf(false) }

    // Seeing this screen once is enough: widgets stop suggesting the calendar.
    LaunchedEffect(Unit) { settings.calendarPromptDismissed = true }
    // Coming back from system settings may have changed the permission.
    LifecycleResumeEffect(Unit) {
        granted = graph.calendar.hasPermission()
        onPauseOrDispose { }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
        if (ok) {
            graph.io.execute { Scheduler.ensure(context.applicationContext) }
            WidgetUpdater.requestUpdate(context)
        }
    }

    val calendars by produceState(initialValue = emptyList<CalendarInfo>(), granted, version) {
        value = if (granted) withContext(Dispatchers.IO) { graph.calendar.calendars() } else emptyList()
    }

    fun changed() = WidgetUpdater.requestUpdate(context)

    Page(title = stringResource(R.string.calendar_title), onBack = { navigator.pop() }, eyebrow = stringResource(R.string.calendar_eyebrow)) {
        if (!granted) {
            item {
                Panel(Modifier.padding(top = 8.dp)) {
                    Column {
                        Text(stringResource(R.string.calendar_connect_title), style = MaterialTheme.typography.headlineSmall)
                        Body(stringResource(R.string.calendar_connect_body), Modifier.padding(top = 6.dp))
                        PrimaryButton(
                            stringResource(R.string.calendar_allow),
                            { launcher.launch(Manifest.permission.READ_CALENDAR) },
                            Modifier.padding(top = 16.dp),
                        )
                        if (denied) {
                            Body(stringResource(R.string.calendar_denied), Modifier.padding(top = 12.dp))
                            QuietButton(
                                stringResource(R.string.action_open_settings),
                                {
                                    context.startActivity(
                                        Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                },
                                Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
            return@Page
        }

        item { Section(stringResource(R.string.calendar_section_calendars)) }
        if (calendars.isEmpty()) {
            item { Body(stringResource(R.string.calendar_none_found)) }
        }
        val selected = settings.calendarIds
        items(calendars, key = { it.id }) { calendar ->
            val checked = selected?.contains(calendar.id) ?: calendar.isVisible
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checked,
                        role = Role.Checkbox,
                        onValueChange = { on ->
                            // First change turns "all visible calendars" into an explicit list.
                            val base = settings.calendarIds ?: calendars.filter { it.isVisible }.map { it.id }.toSet()
                            settings.calendarIds = if (on) base + calendar.id else base - calendar.id
                            changed()
                        },
                    )
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(Color(calendar.color)))
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(calendar.name, style = MaterialTheme.typography.titleMedium)
                    calendar.accountName?.let { Body(it) }
                }
                Checkbox(checked = checked, onCheckedChange = null)
            }
        }

        item { Section(stringResource(R.string.calendar_section_show)) }
        item {
            val allDay = remember(version) { settings.includeAllDay }
            ToggleRow(stringResource(R.string.calendar_all_day), null, allDay) { settings.includeAllDay = it; changed() }
        }
        item {
            val hideDeclined = remember(version) { settings.hideDeclined }
            ToggleRow(stringResource(R.string.calendar_hide_declined), null, hideDeclined) { settings.hideDeclined = it; changed() }
        }
        item {
            val merge = remember(version) { settings.mergeDuplicates }
            ToggleRow(stringResource(R.string.calendar_merge), stringResource(R.string.calendar_merge_detail), merge) { settings.mergeDuplicates = it; changed() }
        }
        item {
            val titles = remember(version) { settings.showTitles }
            ToggleRow(stringResource(R.string.calendar_show_titles), stringResource(R.string.calendar_show_titles_detail), titles) { settings.showTitles = it; changed() }
        }

        item { Section(stringResource(R.string.calendar_section_range)) }
        item {
            val days = remember(version) { settings.daysAhead }
            Choices(
                options = listOf(1, 3, 7, 14).map { it to resources.getQuantityString(R.plurals.calendar_days, it, it) },
                selected = days,
                onSelect = { settings.daysAhead = it; changed() },
            )
        }
        item { Body(stringResource(R.string.calendar_privacy_note), Modifier.padding(top = 24.dp)) }
    }
}
