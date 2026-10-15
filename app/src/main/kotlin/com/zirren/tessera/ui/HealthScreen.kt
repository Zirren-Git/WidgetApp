package com.zirren.tessera.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirren.tessera.R
import com.zirren.tessera.core.weather.RefreshTrigger
import com.zirren.tessera.data.Check
import com.zirren.tessera.data.Fix
import com.zirren.tessera.data.HealthChecks
import com.zirren.tessera.graph
import com.zirren.tessera.widget.TodayWidgetProvider
import com.zirren.tessera.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** "Why isn't my widget updating?", answered by checking everything and offering the fix. */
@Composable
fun HealthScreen(navigator: Navigator, version: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val checks by produceState(initialValue = emptyList<Check>(), version) {
        value = withContext(Dispatchers.IO) { HealthChecks.run(context) }
    }

    fun fix(fix: Fix) {
        when (fix) {
            Fix.SET_PLACE, Fix.ALLOW_LOCATION -> navigator.push(Screen.Weather)
            Fix.CONNECT_CALENDAR -> navigator.push(Screen.Calendar)
            Fix.ADD_WIDGET -> requestPin(context, TodayWidgetProvider::class.java)
            Fix.REFRESH_WEATHER -> scope.launch {
                withContext(Dispatchers.IO) { context.graph.weather.refresh(RefreshTrigger.USER) }
                WidgetUpdater.requestUpdate(context)
            }
            Fix.APP_SETTINGS -> context.startActivity(
                Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    val problems = checks.count { !it.ok }
    Page(title = stringResource(R.string.health_title), onBack = { navigator.pop() }, eyebrow = stringResource(R.string.health_eyebrow)) {
        item {
            Body(
                if (problems == 0) stringResource(R.string.health_all_good)
                else context.resources.getQuantityString(R.plurals.home_health_issues, problems, problems),
                Modifier.padding(bottom = 12.dp),
                color = if (problems == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            )
            Hairline()
        }
        items(checks) { check ->
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    painterResource(if (check.ok) R.drawable.ic_check else R.drawable.ic_alert),
                    contentDescription = stringResource(if (check.ok) R.string.health_ok else R.string.health_needs_attention),
                    tint = if (check.ok) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(check.title, style = MaterialTheme.typography.titleMedium)
                    Body(check.detail, Modifier.padding(top = 2.dp))
                    check.fix?.let { action ->
                        QuietButton(stringResource(fixLabel(action)), { fix(action) }, Modifier.padding(top = 10.dp))
                    }
                }
            }
            Hairline()
        }
    }
}

private fun fixLabel(fix: Fix): Int = when (fix) {
    Fix.SET_PLACE -> R.string.fix_set_place
    Fix.REFRESH_WEATHER -> R.string.fix_refresh
    Fix.ALLOW_LOCATION -> R.string.fix_location
    Fix.CONNECT_CALENDAR -> R.string.fix_calendar
    Fix.APP_SETTINGS -> R.string.fix_app_settings
    Fix.ADD_WIDGET -> R.string.fix_add_widget
}
