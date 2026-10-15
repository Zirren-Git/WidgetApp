package com.zirren.tessera.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirren.tessera.R
import com.zirren.tessera.core.model.Place
import com.zirren.tessera.core.model.TemperatureUnit
import com.zirren.tessera.core.model.UnitPreferences
import com.zirren.tessera.core.model.WindSpeedUnit
import com.zirren.tessera.core.weather.PlaceSearchOutcome
import com.zirren.tessera.core.weather.RefreshTrigger
import com.zirren.tessera.data.Settings
import com.zirren.tessera.data.Texts
import com.zirren.tessera.data.WeatherRepository
import com.zirren.tessera.graph
import com.zirren.tessera.sync.Scheduler
import com.zirren.tessera.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WeatherScreen(navigator: Navigator, version: Int) {
    val context = LocalContext.current
    val graph = context.graph
    val settings = graph.settings
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val place = remember(version) { settings.activePlace() }
    val mode = remember(version) { settings.locationMode }
    val regionDefaults = remember { UnitPreferences.defaultsFor(Texts.regionOf(context)) }

    fun useMyLocation() {
        busy = true
        message = null
        scope.launch {
            val found = withContext(Dispatchers.IO) { locateAndSave(context) }
            busy = false
            message = if (found) null else context.getString(R.string.weather_location_failed)
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            useMyLocation()
        } else {
            message = context.getString(R.string.weather_location_denied)
        }
    }

    Page(title = stringResource(R.string.weather_title), onBack = { navigator.pop() }, eyebrow = stringResource(R.string.weather_eyebrow)) {
        item { Section(stringResource(R.string.weather_section_place)) }
        item {
            Panel {
                Column {
                    Text(
                        place?.label ?: stringResource(R.string.weather_no_place),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Body(
                        when (mode) {
                            Settings.LocationMode.PLACE -> stringResource(R.string.weather_mode_place)
                            Settings.LocationMode.DEVICE -> stringResource(R.string.weather_mode_device)
                            Settings.LocationMode.NONE -> stringResource(R.string.weather_mode_none)
                        },
                        Modifier.padding(top = 4.dp),
                    )
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(stringResource(R.string.weather_search_place), { navigator.push(Screen.PlaceSearch) })
                        QuietButton(
                            stringResource(R.string.weather_use_my_location),
                            {
                                if (graph.location.hasPermission()) useMyLocation()
                                else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            },
                            enabled = !busy,
                        )
                    }
                    if (busy) {
                        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Body(stringResource(R.string.weather_locating), Modifier.padding(start = 10.dp))
                        }
                    }
                    message?.let { Body(it, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
        item { Body(stringResource(R.string.weather_privacy_note), Modifier.padding(top = 10.dp)) }

        item { Section(stringResource(R.string.weather_section_units)) }
        item {
            val selected = remember(version) { settings.temperatureOverride }
            Label(stringResource(R.string.weather_temperature), Modifier.padding(bottom = 8.dp))
            Choices(
                options = listOf<Pair<TemperatureUnit?, String>>(
                    null to stringResource(R.string.units_auto, regionDefaults.temperature.symbol),
                    TemperatureUnit.CELSIUS to TemperatureUnit.CELSIUS.symbol,
                    TemperatureUnit.FAHRENHEIT to TemperatureUnit.FAHRENHEIT.symbol,
                ),
                selected = selected,
                onSelect = { settings.temperatureOverride = it; WidgetUpdater.requestUpdate(context) },
            )
        }
        item {
            val selected = remember(version) { settings.windOverride }
            Label(stringResource(R.string.weather_wind), Modifier.padding(top = 18.dp, bottom = 8.dp))
            Choices(
                options = listOf<Pair<WindSpeedUnit?, String>>(null to stringResource(R.string.units_auto, regionDefaults.windSpeed.symbol)) +
                    WindSpeedUnit.entries.map { it to it.symbol },
                selected = selected,
                onSelect = { settings.windOverride = it; WidgetUpdater.requestUpdate(context) },
            )
        }

        item { Section(stringResource(R.string.weather_section_refresh)) }
        item {
            var refreshNote by remember { mutableStateOf<String?>(null) }
            Column {
                QuietButton(
                    stringResource(R.string.weather_refresh_now),
                    {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { graph.weather.refresh(RefreshTrigger.USER) }
                            refreshNote = describe(context, result)
                            WidgetUpdater.requestUpdate(context)
                        }
                    },
                    enabled = place != null,
                )
                refreshNote?.let { Body(it, Modifier.padding(top = 10.dp)) }
                Body(stringResource(R.string.weather_refresh_note), Modifier.padding(top = 10.dp))
            }
        }

        item { Section(stringResource(R.string.weather_section_sources)) }
        item { Body(stringResource(R.string.weather_attribution)) }
    }
}

@Composable
fun PlaceSearchScreen(navigator: Navigator) {
    val context = LocalContext.current
    val graph = context.graph
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            status = null
            return@LaunchedEffect
        }
        delay(350) // wait until typing pauses; a new keystroke cancels this effect
        status = context.getString(R.string.search_searching)
        val language = Texts.forContext(context, graph.settings).dateTime.locale.language
        when (val outcome = withContext(Dispatchers.IO) { graph.placeSearch.search(query, language) }) {
            is PlaceSearchOutcome.Found -> {
                results = outcome.places
                status = if (outcome.places.isEmpty()) context.getString(R.string.search_no_results) else null
            }
            is PlaceSearchOutcome.Failed -> status = context.getString(R.string.search_failed, outcome.message)
        }
    }

    fun choose(place: Place) {
        val app = context.applicationContext
        graph.settings.place = place
        graph.settings.locationMode = Settings.LocationMode.PLACE
        scope.launch(Dispatchers.IO) {
            graph.weather.refresh(RefreshTrigger.LOCATION_CHANGED)
            Scheduler.ensure(app)
            WidgetUpdater.updateAll(app)
        }
        navigator.pop()
    }

    Page(title = stringResource(R.string.search_title), onBack = { navigator.pop() }) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            )
        }
        status?.let { item { Body(it, Modifier.padding(top = 14.dp)) } }
        items(results, key = { it.point.key() + it.name }) { place ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { choose(place) }
                    .padding(vertical = 14.dp),
            ) {
                Text(place.name, style = MaterialTheme.typography.titleMedium)
                Body(listOfNotNull(place.region, place.country).joinToString(", "))
            }
            Hairline()
        }
        item { Label(stringResource(R.string.search_attribution), Modifier.padding(top = 18.dp)) }
    }
}

/** Finds the phone, names the town, stores it rounded to ~1 km, and fetches weather. */
private fun locateAndSave(context: Context): Boolean {
    val graph = context.graph
    val point = graph.location.locate() ?: return false
    val locale = Texts.forContext(context, graph.settings).dateTime.locale
    val name = graph.location.placeName(point, locale) ?: context.getString(R.string.weather_my_location)
    graph.settings.devicePlace = Place(name = name, point = point)
    graph.settings.locationMode = Settings.LocationMode.DEVICE
    graph.weather.refresh(RefreshTrigger.LOCATION_CHANGED)
    Scheduler.ensure(context.applicationContext)
    WidgetUpdater.updateAll(context.applicationContext)
    return true
}

private fun describe(context: Context, result: WeatherRepository.Result): String = when (result) {
    WeatherRepository.Result.Updated -> context.getString(R.string.refresh_updated)
    is WeatherRepository.Result.UpToDate -> context.getString(R.string.refresh_up_to_date)
    is WeatherRepository.Result.Waiting -> context.getString(R.string.refresh_waiting, result.reason)
    WeatherRepository.Result.NoPlace -> context.getString(R.string.weather_no_place)
    is WeatherRepository.Result.Failed -> context.getString(R.string.refresh_failed, result.message)
}
