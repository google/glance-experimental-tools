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

package com.google.android.glance.appwidget.host.glance

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextDirection.Companion.Content
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.SizeMode
import com.google.android.glance.appwidget.host.getSingleSize
import com.google.android.glance.appwidget.host.toSizeF
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.ceil

@ExperimentalGlanceRemoteViewsApi
internal val glanceRemoteViews by lazy { GlanceRemoteViews() }

/**
 * Extension method to compose a GlanceAppWidget instance into RemoteViews respecting the defined
 * [SizeMode].
 *
 * @return the generated RemoteViews
 */
@ExperimentalGlanceRemoteViewsApi
suspend fun GlanceAppWidget.compose(
    context: Context,
    size: DpSize,
    state: Any? = null,
    info: AppWidgetProviderInfo? = null
): RemoteViews = when (val mode = sizeMode) {
    SizeMode.Single -> compose(context, info?.getSingleSize(context) ?: size, state)
    SizeMode.Exact -> compose(context, size, state)
    is SizeMode.Responsive -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        composeResponsive(context, mode.sizes, state)
    } else {
        compose(context, findBestSize(size, mode.sizes), state)
    }
}

@ExperimentalGlanceRemoteViewsApi
@RequiresApi(Build.VERSION_CODES.S)
private suspend fun GlanceAppWidget.composeResponsive(
    context: Context,
    sizes: Set<DpSize>,
    state: Any?
) = coroutineScope {
    val allViews = sizes.map { size ->
        async {
            size.toSizeF() to compose(context, size, state)
        }
    }.awaitAll()
    allViews.singleOrNull()?.second ?: RemoteViews(allViews.toMap())
}

@ExperimentalGlanceRemoteViewsApi
private suspend fun GlanceAppWidget.compose(
    context: Context,
    size: DpSize,
    state: Any?
): RemoteViews = glanceRemoteViews.compose(
    context = context,
    size = size,
    state = state,
    content = { Content() }
).remoteViews

@Composable
private fun Content() {
}

private fun Collection<DpSize>.sortedBySize() = sortedWith(
    compareBy({ it.width.value * it.height.value }, { it.width.value })
)

// True if the object fits in the given size.
private infix fun DpSize.fitsIn(other: DpSize) =
    (ceil(other.width.value) + 1 > width.value) &&
        (ceil(other.height.value) + 1 > height.value)

private fun squareDistance(widgetSize: DpSize, layoutSize: DpSize): Float {
    val dw = widgetSize.width.value - layoutSize.width.value
    val dh = widgetSize.height.value - layoutSize.height.value
    return dw * dw + dh * dh
}

private fun findBestSize(widgetSize: DpSize, layoutSizes: Collection<DpSize>): DpSize =
    layoutSizes.mapNotNull { layoutSize ->
        if (layoutSize fitsIn widgetSize) {
            layoutSize to squareDistance(widgetSize, layoutSize)
        } else {
            null
        }
    }.minByOrNull { it.second }?.first ?: layoutSizes.sortedBySize().first()
