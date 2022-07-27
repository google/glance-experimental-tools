package com.google.android.glance.tools.preview.internal.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val Context.widgetCornerRadiiPx: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        resources.getDimensionPixelSize(android.R.dimen.system_app_widget_background_radius)
    } else {
        (16 * resources.displayMetrics.density).toInt()
    }

internal val Context.widgetCornerRadiiDp: Dp
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val size =
            resources.getDimensionPixelSize(android.R.dimen.system_app_widget_background_radius)
        (size / resources.displayMetrics.density).dp
    } else {
        16.dp
    }