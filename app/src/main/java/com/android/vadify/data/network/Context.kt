package com.android.vadify.data.network

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes


fun Context.getStringOrDefault(
    @StringRes stringRes: Int,
    default: String?
): String? {
    @Suppress("SwallowedException")
    return try {
        getString(stringRes)
    } catch (e: Resources.NotFoundException) {
        default
    }
}
