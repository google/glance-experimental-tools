/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.glance.appwidget.host

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.SizeF
import android.util.TypedValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.min

public fun DpSize.toSizeF(): SizeF = SizeF(width.value, height.value)

public fun Dp.toPixels(context: Context): Int = toPixels(context.resources.displayMetrics)

public fun Dp.toPixels(displayMetrics: DisplayMetrics): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics).toInt()

internal fun Int.pixelsToDp(context: Context) = pixelsToDp(context.resources.displayMetrics)

internal fun Int.pixelsToDp(displayMetrics: DisplayMetrics) = (this / displayMetrics.density).dp

public fun AppWidgetProviderInfo.getTargetSize(context: Context): DpSize = DpSize(
    minWidth.pixelsToDp(context),
    minHeight.pixelsToDp(context)
)

public fun AppWidgetProviderInfo.getMaxSize(context: Context): DpSize =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && maxResizeWidth > 0) {
        DpSize(
            maxResizeWidth.pixelsToDp(context),
            maxResizeHeight.pixelsToDp(context)
        )
    } else {
        DpSize(Int.MAX_VALUE.dp, Int.MAX_VALUE.dp)
    }

public fun AppWidgetProviderInfo.getMinSize(context: Context): DpSize = DpSize(
    width = minResizeWidth.pixelsToDp(context),
    height = minResizeHeight.pixelsToDp(context)
)

public fun AppWidgetProviderInfo.getSingleSize(context: Context): DpSize {
    val minWidth = min(
        minWidth,
        if (resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0) {
            minResizeWidth
        } else {
            Int.MAX_VALUE
        }
    )
    val minHeight = min(
        minHeight,
        if (resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0) {
            minResizeHeight
        } else {
            Int.MAX_VALUE
        }
    )
    return DpSize(
        minWidth.pixelsToDp(context),
        minHeight.pixelsToDp(context)
    )
}

public val Context.appwidgetBackgroundRadius: Dp
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val size = resources.getDimensionPixelSize(
            android.R.dimen.system_app_widget_background_radius
        )
        (size / resources.displayMetrics.density).dp
    } else {
        16.dp
    }

public val Context.appwidgetBackgroundRadiusPixels: Float
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        resources.getDimensionPixelSize(
            android.R.dimen.system_app_widget_background_radius
        ).toFloat()
    } else {
        (16 * resources.displayMetrics.density)
    }

public fun AppWidgetProviderInfo.toSizeExtras(context: Context, availableSize: DpSize): Bundle {
    return Bundle().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                minResizeWidth
            )
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                minResizeHeight
            )
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                maxResizeWidth
            )
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                maxResizeHeight
            )
            putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                arrayListOf(availableSize.toSizeF())
            )
        } else {
            // TODO to check how this affects the different glance SizeModes
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                availableSize.width.toPixels(context)
            )
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                availableSize.height.toPixels(context)
            )
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                availableSize.width.toPixels(context)
            )
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                availableSize.height.toPixels(context)
            )
        }
    }
}
