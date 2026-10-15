package com.zirren.tessera.ui.tools

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirren.tessera.R

/** "+ Add to home screen": pins [tool]'s own icon, or explains where to find it if the launcher can't. */
@Composable
fun PinToolButton(tool: Tool) {
    val context = LocalContext.current
    val unsupported = stringResource(R.string.tools_pin_unsupported)
    TextButton(
        onClick = {
            if (!ToolShortcuts.requestPin(context, tool)) Toast.makeText(context, unsupported, Toast.LENGTH_LONG).show()
        },
    ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.home_add_to_home_screen), style = MaterialTheme.typography.labelLarge)
    }
}

/** Back arrow and the pin action, for tool screens that don't scroll (like the calculator). */
@Composable
fun ToolTopBar(tool: Tool, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
        }
        Spacer(Modifier.weight(1f))
        PinToolButton(tool)
    }
}
