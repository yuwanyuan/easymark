package com.easymd.data

import android.content.Context
import android.content.SharedPreferences
import com.easymd.ui.components.MarkdownSyntax

object ToolbarSettingsRepository {

    private const val PREFS_NAME = "toolbar_settings"
    private const val KEY_ENABLED_ITEMS = "enabled_items"

    val allSyntaxItems: List<MarkdownSyntax> = MarkdownSyntax.entries

    val defaultEnabledItems: Set<String> = allSyntaxItems.map { it.name }.toSet()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getEnabledItems(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_ENABLED_ITEMS, defaultEnabledItems)
            ?: defaultEnabledItems
    }

    fun setEnabledItems(context: Context, items: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_ENABLED_ITEMS, items).apply()
    }

    fun getEnabledSyntaxList(context: Context): List<MarkdownSyntax> {
        val enabled = getEnabledItems(context)
        return allSyntaxItems.filter { it.name in enabled }
    }
}
