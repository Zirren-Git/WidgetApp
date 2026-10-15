package com.zirren.tessera.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirren.tessera.R
import com.zirren.tessera.core.model.Age
import com.zirren.tessera.core.weather.DaySummary
import com.zirren.tessera.core.weather.PrecipitationOutlook
import com.zirren.tessera.core.weather.SunEvents
import com.zirren.tessera.data.Texts
import com.zirren.tessera.data.WeatherSnapshot
import com.zirren.tessera.graph
import com.zirren.tessera.widget.WeatherIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

private class ForecastData(val snapshot: WeatherSnapshot?, val configured: Boolean, val texts: Texts, val now: Instant, val zone: ZoneId)

@Composable
fun ForecastScreen(navigator: Navigator, version: Int) {
    val context = LocalContext.current
    val graph = context.graph
    val data by produceState<ForecastData?>(initialValue = null, version) {
        value = withContext(Dispatchers.IO) {
            val now = Instant.now()
            val zone = ZoneId.systemDefault()
            ForecastData(graph.weather.snapshot(now, zone), graph.settings.activePlace() != null, Texts.forContext(context, graph.settings), now, zone)
        }
    }
    val snapshot = data?.snapshot
    val texts = data?.texts

    Page(
        title = snapshot?.place?.label ?: stringResource(R.string.forecast_title),
        onBack = { navigator.pop() },
        eyebrow = if (snapshot != null && data != null) {
            stringResource(R.string.forecast_updated, data!!.texts.age.format(Age.between(snapshot.fetchedAt, data!!.now)))
        } else {
            stringResource(R.string.forecast_eyebrow)
        },
    ) {
        val loaded = data ?: return@Page
        if (!loaded.configured) {
            item {
                Body(stringResource(R.string.forecast_no_place), Modifier.padding(top = 8.dp))
                PrimaryButton(stringResource(R.string.weather_search_place), { navigator.push(Screen.Weather) }, Modifier.padding(top = 14.dp))
            }
            return@Page
        }
        val current = snapshot?.report?.current
        if (snapshot == null || current == null || texts == null) {
            item { Body(stringResource(R.string.forecast_loading), Modifier.padding(top = 8.dp)) }
            return@Page
        }
        val report = snapshot.report
        val text = texts.weather

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text(text.temperature(current.temperature), style = MaterialTheme.typography.displayLarge, modifier = Modifier.weight(1f))
                Image(painterResource(WeatherIcons.drawable(current.symbol?.icon)), contentDescription = current.symbol?.label, modifier = Modifier.size(64.dp))
            }
            Text(current.symbol?.label.orEmpty(), style = MaterialTheme.typography.titleLarge)
            val details = listOfNotNull(
                text.feelsLike(current.feelsLike),
                current.windSpeed?.let { text.wind(it, current.windFromDirection) },
                current.humidity?.let { stringResource(R.string.forecast_humidity, it.roundToInt()) },
                current.uvIndex?.takeIf { it >= 1 }?.let { stringResource(R.string.forecast_uv, it.roundToInt()) },
            )
            Body(details.joinToString(" · "), Modifier.padding(top = 4.dp))
            report.outlook?.let { outlook ->
                val wet = outlook !is PrecipitationOutlook.DryFor
                Text(
                    text.outlook(outlook),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (wet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }

        item { Section(stringResource(R.string.forecast_section_hours)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                items(report.hourly) { hour ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(texts.dateTime.hour(hour.time.atZone(loaded.zone).toLocalTime()))
                        Image(painterResource(WeatherIcons.drawable(hour.symbol?.icon)), contentDescription = hour.symbol?.label, modifier = Modifier.padding(vertical = 6.dp).size(26.dp))
                        Text(hour.temperature?.let(text::temperature) ?: "\u2013", style = MaterialTheme.typography.titleMedium)
                        val chance = hour.precipitationProbability?.roundToInt()
                        Label(if (chance != null && chance >= 10) "$chance%" else " ", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item { Section(stringResource(R.string.forecast_section_days)) }
        val lows = report.daily.mapNotNull { it.low }
        val highs = report.daily.mapNotNull { it.high }
        val minAll = lows.minOrNull() ?: 0.0
        val maxAll = highs.maxOrNull() ?: 1.0
        items(report.daily) { day ->
            DayRow(day, texts, loaded, minAll, maxAll)
            Hairline()
        }

        report.sun?.let { sun ->
            item { Section(stringResource(R.string.forecast_section_sun)) }
            item {
                Body(
                    when (sun) {
                        is SunEvents.RiseAndSet -> stringResource(
                            R.string.forecast_sun_times,
                            texts.dateTime.time(sun.sunrise.atZone(loaded.zone).toLocalTime()),
                            texts.dateTime.time(sun.sunset.atZone(loaded.zone).toLocalTime()),
                        )
                        SunEvents.PolarDay -> stringResource(R.string.forecast_polar_day)
                        SunEvents.PolarNight -> stringResource(R.string.forecast_polar_night)
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            Column(Modifier.padding(top = 32.dp)) {
                Hairline()
                Label(stringResource(R.string.forecast_attribution), Modifier.padding(top = 14.dp))
            }
        }
    }
}

/** Weekday, icon, and a low–high bar placed on the week's overall range. */
@Composable
private fun DayRow(day: DaySummary, texts: Texts, data: ForecastData, minAll: Double, maxAll: Double) {
    val today = data.now.atZone(data.zone).toLocalDate()
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (day.date == today) stringResource(R.string.agenda_today) else texts.dateTime.shortWeekday(day.date),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(72.dp),
        )
        Image(painterResource(WeatherIcons.drawable(day.symbol?.icon)), contentDescription = day.symbol?.label, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Text(day.low?.let(texts.weather::temperature) ?: "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp))
        RangeBar(day.low, day.high, minAll, maxAll, Modifier.weight(1f))
        Text(day.high?.let(texts.weather::temperature) ?: "", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 10.dp).width(40.dp))
    }
}

@Composable
private fun RangeBar(low: Double?, high: Double?, minAll: Double, maxAll: Double, modifier: Modifier = Modifier) {
    val span = (maxAll - minAll).takeIf { it > 0.1 } ?: 1.0
    BoxWithConstraints(modifier.height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outlineVariant)) {
        if (low != null && high != null) {
            val start = ((low - minAll) / span).toFloat().coerceIn(0f, 1f)
            val end = ((high - minAll) / span).toFloat().coerceIn(start, 1f)
            Box(
                Modifier
                    .offset(x = maxWidth * start)
                    .width(maxWidth * (end - start).coerceAtLeast(0.04f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
