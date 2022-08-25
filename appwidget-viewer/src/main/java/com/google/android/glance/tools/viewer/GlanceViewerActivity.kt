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

package com.google.android.glance.tools.viewer

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.SizeMode
import com.google.android.glance.appwidget.host.getSingleSize
import com.google.android.glance.appwidget.host.getTargetSize
import com.google.android.glance.appwidget.host.toSizeF
import com.google.android.glance.tools.viewer.internal.AppWidgetViewerManager
import com.google.android.glance.tools.viewer.internal.ui.ViewerScreen
import com.google.android.glance.tools.viewer.internal.ui.theme.ViewerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/**
 * Base class to display AppWidgets.
 */
abstract class AppWidgetViewerActivity : ComponentActivity() {

    /**
     * The list of [AppWidgetProvider] to display in the viewer
     */
    abstract fun getProviders(): List<Class<out AppWidgetProvider>>

    /**
     * Provides the [RemoteViews] snapshot of the given [AppWidgetProviderInfo] for the given size
     *
     * @param - The [AppWidgetProviderInfo] containing the metadata of the appwidget
     * @param - The available size to display the appwidget
     *
     * @return the [RemoteViews] instance to use for the viewer.
     */
    abstract suspend fun getAppWidgetSnapshot(
        info: AppWidgetProviderInfo,
        size: DpSize
    ): RemoteViews

    // Moving states outside of composition to ensure they are kept when Live Edits happen.
    private lateinit var selectedProvider: MutableState<AppWidgetProviderInfo>
    private lateinit var currentSize: MutableState<DpSize>

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewerManager = AppWidgetViewerManager(this, getProviders())
        val providers = viewerManager.getProvidersInfo()
        selectedProvider = mutableStateOf(providers.first())
        currentSize = mutableStateOf(selectedProvider.value.getTargetSize(this))

        setContent {
            ViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ViewerScreen(
                        providers = providers,
                        selectedProvider = selectedProvider.value,
                        currentSize = currentSize.value,
                        snapshot = ::getAppWidgetSnapshot,
                        exportSnapshot = {
                            viewerManager.exportSnapshot(
                                selectedProvider.value,
                                it
                            )
                        },
                        onResize = { currentSize.value = it },
                        onSelected = { selectedProvider.value = it },
                        onPin = { providerInfo ->
                            viewerManager.requestPin(
                                providerInfo,
                                getAppWidgetSnapshot(
                                    info = providerInfo,
                                    size = providerInfo.getTargetSize(this)
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Extend this activity to provide a set of GlanceAppWidget snapshots to display.
 */
@ExperimentalGlanceRemoteViewsApi
abstract class GlanceViewerActivity : AppWidgetViewerActivity() {

    private val glanceRemoteViews = GlanceRemoteViews()

    /**
     * Provides an instance of [GlanceAppWidget] to display inside the viewer.
     *
     * @param receiver - The selected [GlanceAppWidgetReceiver] to display
     */
    abstract suspend fun getGlanceSnapshot(receiver: Class<out GlanceAppWidgetReceiver>): GlanceSnapshot

    /**
     * Only override this method to directly provide [RemoteViews] instead of [GlanceAppWidget]
     * instances.
     *
     * @see AppWidgetViewerActivity.getAppWidgetSnapshot
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun getAppWidgetSnapshot(
        info: AppWidgetProviderInfo,
        size: DpSize
    ): RemoteViews = withContext(Dispatchers.IO) {
        val receiver = Class.forName(info.provider.className)
        require(GlanceAppWidgetReceiver::class.java.isAssignableFrom(receiver)) {
            "AppWidget is not a GlanceAppWidgetReceiver. Override this method to provide other implementations"
        }

        val receiverClass = receiver as Class<out GlanceAppWidgetReceiver>
        val snapshot = getGlanceSnapshot(receiverClass)
        snapshot.instance.asRemoteViews(snapshot.state, info, size)
    }

    private suspend fun GlanceAppWidget.asRemoteViews(
        state: Any?,
        info: AppWidgetProviderInfo,
        availableSize: DpSize
    ): RemoteViews = when (val mode = sizeMode) {
        SizeMode.Single -> compose(info.getSingleSize(applicationContext), state)
        SizeMode.Exact -> compose(availableSize, state)
        is SizeMode.Responsive -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            composeResponsive(mode.sizes, state)
        } else {
            compose(findBestSize(availableSize, mode.sizes), state)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun GlanceAppWidget.composeResponsive(
        sizes: Set<DpSize>,
        state: Any?
    ) = coroutineScope {
        val allViews = sizes.map { size ->
            async {
                size.toSizeF() to compose(size, state)
            }
        }.awaitAll()
        allViews.singleOrNull()?.second ?: RemoteViews(allViews.toMap())
    }

    private suspend fun GlanceAppWidget.compose(
        size: DpSize,
        state: Any?
    ): RemoteViews = glanceRemoteViews.compose(
        context = applicationContext,
        size = size,
        state = state,
        content = { Content() }
    ).remoteViews

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
}
