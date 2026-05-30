package com.easymd.data

import android.content.Context

object LayoutSettings {
    private const val PREFS_NAME = "easymd_layout"
    private const val KEY_TABLET_COLUMNS = "tablet_columns"

    fun getTabletColumns(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        return prefs.getInt(KEY_TABLET_COLUMNS, 2)
    }

    fun setTabletColumns(context: Context, columns: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        prefs.edit().putInt(KEY_TABLET_COLUMNS, columns).apply()
    }
}
