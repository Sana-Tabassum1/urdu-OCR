package com.urduocr.scanner

import android.content.Context

class ViewPreferenceManager(private val context: Context) {
    private val sharedPref = context.getSharedPreferences("VIEW_PREF", Context.MODE_PRIVATE)

    fun saveViewPreference(isGridView: Boolean) {
        sharedPref.edit().putBoolean("IS_GRID_VIEW", isGridView).apply()
    }

    fun getViewPreference(): Boolean {
        return sharedPref.getBoolean("IS_GRID_VIEW", false) // Default: List View
    }
}