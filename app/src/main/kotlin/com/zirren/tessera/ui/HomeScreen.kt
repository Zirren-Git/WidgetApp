package com.zirren.tessera.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirren.tessera.R
import com.zirren.tessera.data.HealthChecks
import com.zirren.tessera.data.Texts
import com.zirren.tessera.data.WeatherSnapshot
import com.zirren.tessera.graph
import com.zirren.tessera.ui.theme.Fraunces
import com.zirren.tessera.widget.AgendaWidgetProvider
import com.zirren.tessera.widget.TodayWidgetProvider
import com.zirren.tessera.widget.WeatherIcons
import com.zirren.tessera.widget.WidgetModel
import com.zirren.tessera.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

private class HomeData(
    val model: WidgetModel?,
    val todayPlaced: Int,
    val agendaPlaced: Int,
    val issues: Int,
    val calendarCount: Int,
)

@Composable
fun HomeScreen(navigator: Navigator, version: Int) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val graph = context.graph
    val data by produceState<HomeData?>(initialValue = null, version) {
        value = withContext(Dispatchers.IO) {
            HomeData(
                model = WidgetModel.load(context),
                todayPlaced = WidgetUpdater.count(context, TodayWidgetProvider::class.java),
                agendaPlaced = WidgetUpdater.count(context, AgendaWidgetProvider::class.java),
                issues = HealthChecks.run(context).count { !it.ok },
                calendarCount = graph.calendar.calendars().count { it.isVisible },
            )
        }
    }
    var pinHint by remember { mutableStateOf<Int?>(null) }

    Page(title = stringResource(R.string.app_name), onBack = null, eyebrow = stringResource(R.string.home_eyebrow)) {
        item { Body(stringResource(R.string.home_intro), Modifier.padding(bottom = 8.dp)) }

        item { Section(stringResource(R.string.home_section_widgets)) }
        item {
            FeaturedWidgetCard(
                placed = data?.todayPlaced ?: 0,
                model = data?.model,
                onAdd = { pinHint = if (requestPin(context, TodayWidgetProvider::class.java)) null else R.string.home_pin_unsupported },
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            CompactWidgetCard(
                name = stringResource(R.string.widget_agenda_name),
                description = stringResource(R.string.widget_agenda_description),
                placed = data?.agendaPlaced ?: 0,
                onAdd = { pinHint = if (requestPin(context, AgendaWidgetProvider::class.java)) null else R.string.home_pin_unsupported },
            )
        }
        pinHint?.let { hint -> item { Body(stringResource(hint), Modifier.padding(top = 12.dp)) } }

        item { Section(stringResource(R.string.home_section_sources)) }
        item {
            val weather = data?.model?.weather
            NavRow(
                icon = R.drawable.ic_location,
                title = stringResource(R.string.home_weather),
                value = weatherSummary(resources, data?.model, weather, graph.settings.activePlace()?.label),
                onClick = { navigator.push(Screen.Weather) },
            )
        }
        item { Hairline() }
        item {
            NavRow(
                icon = R.drawable.ic_calendar,
                title = stringResource(R.string.home_calendar),
                value = calendarSummary(resources, data),
                onClick = { navigator.push(Screen.Calendar) },
            )
        }
        item { Hairline() }
        item {
            val issues = data?.issues ?: 0
            NavRow(
                icon = if (issues == 0) R.drawable.ic_check else R.drawable.ic_alert,
                title = stringResource(R.string.home_health),
                value = if (issues == 0) stringResource(R.string.home_health_ok)
                else resources.getQuantityString(R.plurals.home_health_issues, issues, issues),
                onClick = { navigator.push(Screen.Health) },
                iconTint = if (issues == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            )
        }

        if (Build.VERSION.SDK_INT >= 31) {
            item { Section(stringResource(R.string.home_section_appearance)) }
            item {
                val settings = graph.settings
                val wallpaper = remember(version) { settings.useWallpaperColors }
                ToggleRow(
                    title = stringResource(R.string.appearance_wallpaper_colors),
                    detail = stringResource(R.string.appearance_wallpaper_colors_detail),
                    checked = wallpaper,
                    onChange = { settings.useWallpaperColors = it },
                )
            }
        }

        item {
            Column(Modifier.padding(top = 36.dp)) {
                Hairline()
                Label(stringResource(R.string.home_footer), Modifier.padding(top = 14.dp))
                Body(stringResource(R.string.home_footer_detail), Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** The featured card shows a live miniature of the widget, sitting on the "desk". */
@Composable
private fun FeaturedWidgetCard(placed: Int, model: WidgetModel?, onAdd: () -> Unit) {
    Panel {
        Column {
            PlacedLabel(placed)
            Text(stringResource(R.string.widget_today_name), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
            Body(stringResource(R.string.widget_today_description), Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(16.dp))
            MorningPaperMiniature(model)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(stringResource(R.string.home_add_to_home_screen), onAdd)
        }
    }
}

@Composable
private fun CompactWidgetCard(name: String, description: String, placed: Int, onAdd: () -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                PlacedLabel(placed)
                Text(name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 6.dp))
                Body(description, Modifier.padding(top = 2.dp))
            }
            QuietButton(stringResource(R.string.home_add_short), onAdd)
        }
    }
}

@Composable
private fun PlacedLabel(placed: Int) {
    val resources = LocalResources.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LiveDot(live = placed > 0)
        Label(
            if (placed > 0) resources.getQuantityString(R.plurals.home_placed, placed, placed)
            else stringResource(R.string.home_not_placed),
            color = if (placed > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A miniature of "Morning Paper" with the user's real data. The one shadow in the app:
 * a single offset shape, as if lit by a desk lamp at the top left.
 */
@Composable
private fun MorningPaperMiniature(model: WidgetModel?) {
    val clock by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(15_000)
        }
    }
    val texts = model?.texts
    val current = model?.weather?.report?.current
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 5.dp)) {
        Box(Modifier.matchParentSize().offset(x = 4.dp, y = 5.dp).clip(shape).background(Color.Black.copy(alpha = 0.10f)))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(16.dp),
        ) {
            Label(texts?.dateTime?.longDate(LocalDate.now()) ?: "")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    texts?.dateTime?.time(clock.withSecond(0).withNano(0)) ?: "",
                    fontFamily = Fraunces,
                    fontSize = 44.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (current != null && texts != null) {
                    Image(painterResource(WeatherIcons.drawable(current.symbol?.icon)), contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(texts.weather.temperature(current.temperature), fontFamily = Fraunces, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            val next = model?.agenda?.nextUp
            if (model != null && next != null && texts != null) {
                Hairline(Modifier.padding(vertical = 8.dp))
                Row {
                    Text(texts.agenda.whenLabel(next, model.now, model.zone), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(next.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun weatherSummary(resources: Resources, model: WidgetModel?, weather: WeatherSnapshot?, placeLabel: String?): String {
    if (model == null) return ""
    if (!model.weatherConfigured) return resources.getString(R.string.home_weather_not_set)
    val waiting = resources.getString(R.string.home_weather_waiting, placeLabel.orEmpty())
    if (weather == null) return waiting
    val current = weather.report.current ?: return waiting
    val texts: Texts = model.texts
    return listOfNotNull(weather.place.label, texts.weather.temperature(current.temperature), current.symbol?.label).joinToString(" · ")
}

private fun calendarSummary(resources: Resources, data: HomeData?): String {
    if (data == null) return ""
    val model = data.model ?: return ""
    if (!model.calendarAllowed) return resources.getString(R.string.home_calendar_not_connected)
    val calendars = resources.getQuantityString(R.plurals.home_calendar_count, data.calendarCount, data.calendarCount)
    val next = model.agenda?.nextUp ?: return calendars
    return resources.getString(R.string.home_calendar_next, calendars, model.texts.agenda.whenLabel(next, model.now, model.zone), next.title)
}

/** Android 8+ can ask the launcher to place a widget directly: no hunting through the widget list. */
fun requestPin(context: Context, provider: Class<*>): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    return manager.isRequestPinAppWidgetSupported &&
        manager.requestPinAppWidget(ComponentName(context, provider), null, null)
}
