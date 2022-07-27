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

package com.google.android.glance.tools.preview.internal

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.SizeF
import android.util.TypedValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.min

internal fun DpSize.toSizeF(): SizeF = SizeF(width.value, height.value)

internal fun Dp.toPixels(context: Context) = toPixels(context.resources.displayMetrics)

internal fun Dp.toPixels(displayMetrics: DisplayMetrics) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics).toInt()

internal fun Int.pixelsToDp(context: Context) = pixelsToDp(context.resources.displayMetrics)

internal fun Int.pixelsToDp(displayMetrics: DisplayMetrics) = (this / displayMetrics.density).dp

internal fun AppWidgetProviderInfo.getTargetSize(context: Context): DpSize = DpSize(
    minWidth.pixelsToDp(context),
    minHeight.pixelsToDp(context)
)

internal fun AppWidgetProviderInfo.getMaxSize(context: Context): DpSize =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && maxResizeWidth > 0) {
        DpSize(
            maxResizeWidth.pixelsToDp(context),
            maxResizeHeight.pixelsToDp(context)
        )
    } else {
        DpSize(Int.MAX_VALUE.dp, Int.MAX_VALUE.dp)
    }

internal fun AppWidgetProviderInfo.getMinSize(context: Context): DpSize = DpSize(
    width = minResizeWidth.pixelsToDp(context),
    height = minResizeHeight.pixelsToDp(context)
)


internal fun AppWidgetProviderInfo.getSingleSize(context: Context): DpSize {
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