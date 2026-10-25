package com.example.mybase.data.local

import android.content.SharedPreferences

interface StringPreferenceStore {
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
}

class SharedPreferencesStringStore(
    private val sharedPreferences: SharedPreferences
) : StringPreferenceStore {
    override fun getString(key: String, defaultValue: String): String =
        sharedPreferences.getString(key, defaultValue) ?: defaultValue

    override fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }
}
