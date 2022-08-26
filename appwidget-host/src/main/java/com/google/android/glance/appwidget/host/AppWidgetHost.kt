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

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.ActivityInfo
import android.os.Build
import android.widget.RemoteViews
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.Executors

/**
 * State for the [AppWidgetHost] that holds the host view and allows to update the appwidget.
 *
 * @see AppWidgetHost
 */
class AppWidgetHostState(
    val providerInfo: AppWidgetProviderInfo?,
    private val state: MutableState<AppWidgetHostView?>
) {

    /**
     * The current [AppWidgetHostView] instance or null if not laid out yet.
     */
    var value: AppWidgetHostView?
        get() = state.value
        internal set(value) {
            state.value = value
        }

    /**
     * True if the host is ready to display RemoteViews, false otherwise
     */
    val isReady: Boolean
        get() = state.value != null

    /**
     * Holds the last snapshot provided to the host or null if none
     */
    var snapshot: RemoteViews? = null
        internal set

    /**
     * Update the current host (if available) to display the provided [RemoteViews]
     */
    fun updateAppWidget(remoteViews: RemoteViews) {
        val host = value ?: return
        snapshot = remoteViews
        host.updateAppWidget(remoteViews)
    }
}

@Composable
fun rememberAppWidgetHostState(providerInfo: AppWidgetProviderInfo? = null) = remember(providerInfo) {
    AppWidgetHostState(providerInfo, mutableStateOf(null))
}

/**
 * A layout composable with an [AppWidgetHostView] to display the provided [RemoteViews] from the
 * state.
 *
 * @param modifier - The modifier to be applied to the layout.
 * @param displaySize - The available size for the RemoteViews to display by the host
 * @param state - The [AppWidgetHostState] used to notify changes in the host
 * @param gridColor - The color of the grid and widget area lines or null for none.
 */
@Composable
fun AppWidgetHost(
    modifier: Modifier = Modifier,
    displaySize: DpSize,
    state: AppWidgetHostState,
    gridColor: Color? = null
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (gridColor != null) {
            CellGrid(
                rows = 5,
                columns = 5,
                color = gridColor,
                modifier = Modifier.fillMaxSize()
            )
        }

        val hostModifier = if (displaySize != DpSize.Unspecified) {
            Modifier.size(displaySize)
        } else {
            Modifier.fillMaxSize()
        }.run {
            if (gridColor != null) {
                dashedBorder(1.dp, 1.dp, gridColor)
            } else {
                this
            }
        }

        AndroidView(
            factory = { context ->
                AppWidgetHostView(context).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setExecutor(Executors.newSingleThreadExecutor())
                    }
                }
            },
            modifier = hostModifier.clip(
                RoundedCornerShape(
                    CornerSize(LocalContext.current.appwidgetBackgroundRadius)
                )
            ),
            update = { hostView ->
                if (hostView != state.value) {
                    if (state.providerInfo != null) {
                        hostView.setAppWidget(1, state.providerInfo)
                        hostView.setPadding(0, 0, 0, 0)
                    } else {
                        // When no provider is provided, use a fake provider workaround to init the host
                        hostView.setFakeAppWidget()
                    }
                }
                state.value = hostView
            }
        )
    }
}

/**
 * The hostView requires an AppWidgetProviderInfo to work in certain OS versions. This method uses
 * reflection to set a fake provider info.
 */
private fun AppWidgetHostView.setFakeAppWidget() {
    val context = context
    val info = AppWidgetProviderInfo()
    try {
        val activityInfo = ActivityInfo().apply {
            applicationInfo = context.applicationInfo
            packageName = context.packageName
            labelRes = applicationInfo.labelRes
        }

        info::class.java.getDeclaredField("providerInfo").run {
            isAccessible = true
            set(info, activityInfo)
        }

        this::class.java.getDeclaredField("mInfo").apply {
            isAccessible = true
            set(this@setFakeAppWidget, info)
        }
    } catch (e: Exception) {
        throw IllegalStateException("Couldn't not set fake provider", e)
    }
}

@Composable
private fun CellGrid(rows: Int, columns: Int, color: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val cellSize = Size((this.size.width / columns), (this.size.height / rows))
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                translate(
                    left = column * cellSize.width,
                    top = row * cellSize.height
                ) {
                    drawOutline(
                        outline = Outline.Rectangle(
                            Rect(
                                offset = Offset(0f, 0f),
                                size = cellSize
                            )
                        ),
                        color = color,
                        style = Stroke(width = 1f)
                    )
                }
            }
        }
    }
}

private fun Modifier.dashedBorder(width: Dp, radius: Dp, color: Color) =
    drawBehind {
        drawIntoCanvas {
            val paint = Paint()
                .apply {
                    strokeWidth = width.toPx()
                    this.color = color
                    style = PaintingStyle.Stroke
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                }
            it.drawRoundRect(
                width.toPx(),
                width.toPx(),
                size.width - width.toPx(),
                size.height - width.toPx(),
                radius.toPx(),
                radius.toPx(),
                paint
            )
        }
    }
