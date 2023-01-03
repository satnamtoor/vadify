package com.android.vadify.data.service

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import javax.inject.Inject

/**
 * Preference service
 *
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class PreferenceService @Inject constructor(context: Context) {

    private val defaultSharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    private val resources: Resources = context.resources

    operator fun contains(@StringRes resId: Int): Boolean {
        return defaultSharedPreferences.contains(resources.getString(resId))
    }


    fun remove(@StringRes resId: Int) {
        defaultSharedPreferences.edit().remove(resources.getString(resId)).apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return defaultSharedPreferences.getBoolean(key, default)
    }

    fun getBoolean(@StringRes resId: Int, default: Boolean = false): Boolean {
        return defaultSharedPreferences.getBoolean(resources.getString(resId), default)
    }

    fun getBooleanfireBase(item: String, default: Boolean = false): Boolean {
        return defaultSharedPreferences.getBoolean(item, default)
    }

    fun getString(@StringRes resId: Int, default: String? = ""): String? {
        return defaultSharedPreferences.getString(resources.getString(resId), default)
    }

    fun getFireBaseString(resId: String, default: String? = ""): String? {
        return defaultSharedPreferences.getString(resId, default)
    }

    fun putString(@StringRes resId: Int, value: String?) {
        defaultSharedPreferences.edit().putString(resources.getString(resId), value).apply()
    }

    fun getInt(@StringRes resId: Int, default: Int = 0): Int {
        return defaultSharedPreferences.getInt(resources.getString(resId), default)
    }

    fun putInt(@StringRes resId: Int, value: Int) {
        defaultSharedPreferences.edit().putInt(resources.getString(resId), value).apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        defaultSharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun putBoolean(@StringRes resId: Int, value: Boolean) {
        defaultSharedPreferences.edit().putBoolean(resources.getString(resId), value).apply()
    }

    fun putLong(@StringRes resId: Int, value: Long) {
        putLong(resources.getString(resId), value)
    }

    fun putLong(key: String, value: Long) {
        defaultSharedPreferences.edit().putLong(key, value).apply()
    }

    fun getLong(@StringRes resId: Int): Long {
        return getLong(resources.getString(resId))
    }

    fun getLong(key: String, default: Long = 0): Long {
        return defaultSharedPreferences.getLong(key, default)
    }

    fun putFloat(@StringRes resId: Int, value: Float) {
        putFloat(resources.getString(resId), value)
    }

    fun putFloat(key: String, value: Float) {
        defaultSharedPreferences.edit().putFloat(key, value).apply()
    }

    fun getFloat(@StringRes resId: Int): Float {
        return getFloat(resources.getString(resId))
    }

    fun getFloat(key: String, default: Float = 0F): Float {
        return defaultSharedPreferences.getFloat(key, default)
    }

    fun clearPreference() {
        defaultSharedPreferences.edit().clear().apply();
    }


    fun getPhoneNumber(resId: String, default: String? = ""): String? {
        return defaultSharedPreferences.getString(resId, default)
    }

    fun putPhoneNumberString(@StringRes resId: Int, value: String?) {
        defaultSharedPreferences.edit().putString(resources.getString(resId), value).apply()
    }
}
