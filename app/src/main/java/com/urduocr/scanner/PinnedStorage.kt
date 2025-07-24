package com.urduocr.scanner

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.urduocr.scanner.models.InternalFileModel

object PinnedStorage {
    private const val PREF_NAME = "PinnedPrefs"
    private const val KEY_FILES = "PinnedFiles"

    fun savePinnedFiles(context: Context, files: List<InternalFileModel>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(files)
        prefs.edit().putString(KEY_FILES, json).apply()
    }

    fun loadPinnedFiles(context: Context): MutableList<InternalFileModel> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FILES, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<InternalFileModel>>() {}.type
            Gson().fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}