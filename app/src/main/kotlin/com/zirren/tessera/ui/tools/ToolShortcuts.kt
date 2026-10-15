package com.zirren.tessera.ui.tools

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.util.Log
import com.zirren.tessera.widget.WidgetIntents

/**
 * Tools on the home screen without a widget: a tool's own icon ("Add to home screen") and the
 * menu that appears when you long-press Tessera's app icon. Both open tessera://open/<tool>,
 * exactly like a widget tap. No permission is needed for either.
 */
object ToolShortcuts {
    private const val TAG = "TesseraShortcuts"

    /**
     * Refreshes the long-press menu, and the labels of tool icons already on the home screen
     * (they follow the phone's language). Call while an activity is showing, so it isn't rate-limited.
     */
    fun publish(context: Context) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        try {
            manager.setDynamicShortcuts(Tool.entries.filter { it.inAppIconMenu }.map { info(context, it) })
            manager.updateShortcuts(Tool.entries.map { info(context, it) })
        } catch (e: IllegalStateException) {
            // The phone is still locked after a restart; the next app start tries again.
            Log.w(TAG, "Could not publish tool shortcuts", e)
        }
    }

    /** Asks the launcher to put [tool]'s icon on the home screen. False if the launcher can't. */
    fun requestPin(context: Context, tool: Tool): Boolean {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!manager.isRequestPinShortcutSupported) return false
        return try {
            manager.requestPinShortcut(info(context, tool), null)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Launcher refused to pin ${tool.segment}", e)
            false
        }
    }

    private fun info(context: Context, tool: Tool): ShortcutInfo {
        val label = context.getString(tool.title)
        return ShortcutInfo.Builder(context, tool.shortcutId)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(Icon.createWithResource(context, tool.shortcutIcon))
            .setIntent(WidgetIntents.screenIntent(context, tool.segment))
            .setRank(tool.ordinal)
            .build()
    }
}
