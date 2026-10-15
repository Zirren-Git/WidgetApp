package com.zirren.tessera.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirren.tessera.R
import com.zirren.tessera.ui.Body
import com.zirren.tessera.ui.Hairline
import com.zirren.tessera.ui.Label
import com.zirren.tessera.ui.NavRow
import com.zirren.tessera.ui.Navigator
import com.zirren.tessera.ui.Page
import com.zirren.tessera.ui.Panel
import com.zirren.tessera.ui.PrimaryButton
import com.zirren.tessera.ui.Screen
import com.zirren.tessera.ui.Section

/** One featured tool, then a denser list of the rest. */
@Composable
fun ToolsScreen(navigator: Navigator) {
    val featured = Tool.CALCULATOR
    val others = Tool.entries.filter { it != featured }

    Page(title = stringResource(R.string.tools_title), onBack = { navigator.pop() }, eyebrow = stringResource(R.string.tools_eyebrow)) {
        item { Body(stringResource(R.string.tools_intro), Modifier.padding(bottom = 16.dp)) }
        item {
            Panel {
                Column {
                    Icon(
                        painterResource(featured.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(stringResource(featured.title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp))
                    Body(stringResource(featured.summary), Modifier.padding(top = 2.dp))
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PrimaryButton(stringResource(R.string.tools_open), { navigator.push(Screen.ToolPage(featured)) })
                        PinToolButton(featured)
                    }
                }
            }
        }
        if (others.isNotEmpty()) {
            item { Section(stringResource(R.string.tools_section_more)) }
            items(others, key = { it.segment }) { tool ->
                NavRow(
                    icon = tool.icon,
                    title = stringResource(tool.title),
                    value = stringResource(tool.summary),
                    onClick = { navigator.push(Screen.ToolPage(tool)) },
                )
                Hairline()
            }
        }
        item {
            Column(Modifier.padding(top = 36.dp)) {
                Hairline()
                Label(stringResource(R.string.tools_footer), Modifier.padding(top = 14.dp))
                Body(stringResource(R.string.tools_footer_detail), Modifier.padding(top = 4.dp))
            }
        }
    }
}
