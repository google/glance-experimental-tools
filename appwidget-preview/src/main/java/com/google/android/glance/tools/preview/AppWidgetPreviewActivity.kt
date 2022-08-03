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

package com.google.android.glance.tools.preview

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
import com.google.android.glance.tools.preview.internal.AppWidgetPreviewManager
import com.google.android.glance.tools.preview.internal.ui.PreviewScreen
import com.google.android.glance.tools.preview.internal.ui.theme.PreviewTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/**
 * Base class to display preview of AppWidgets.
 */
abstract class AppWidgetPreviewActivity : ComponentActivity() {

    /**
     * The list of [AppWidgetProvider] to display previews
     */
    abstract fun getProviders(): List<Class<out AppWidgetProvider>>

    /**
     * Provides the [RemoteViews] preview of the given [AppWidgetProviderInfo] for the given size
     *
     * @param - The [AppWidgetProviderInfo] containing the metadata of the appwidget
     * @param - The available size to display the appwidget
     *
     * @return the [RemoteViews] instance to use for the preview.
     */
    abstract suspend fun getAppWidgetPreview(
        info: AppWidgetProviderInfo,
        size: DpSize
    ): RemoteViews

    // Moving states outside of composition to ensure they are kept when Live Edits happen.
    private lateinit var selectedProvider: MutableState<AppWidgetProviderInfo>
    private lateinit var currentSize: MutableState<DpSize>

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val previewManager = AppWidgetPreviewManager(this, getProviders())
        val providers = previewManager.getProvidersInfo()
        selectedProvider = mutableStateOf(providers.first())
        currentSize = mutableStateOf(selectedProvider.value.getTargetSize(this))

        setContent {
            PreviewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PreviewScreen(
                        providers = providers,
                        selectedProvider = selectedProvider.value,
                        currentSize = currentSize.value,
                        preview = ::getAppWidgetPreview,
                        exportPreview = {
                            previewManager.exportPreview(
                                selectedProvider.value,
                                it
                            )
                        },
                        onResize = { currentSize.value = it },
                        onSelected = { selectedProvider.value = it }
                    )
                }
            }
        }
    }
}

/**
 * Extend this activity to provide a set of GlanceAppWidget previews to display.
 */
@ExperimentalGlanceRemoteViewsApi
abstract class GlancePreviewActivity : AppWidgetPreviewActivity() {

    /**
     * Provides an instance of [GlanceAppWidget] to display inside the preview.
     *
     * @param receiver - The selected [GlanceAppWidgetReceiver] to display a preview
     */
    abstract suspend fun getGlancePreview(receiver: Class<out GlanceAppWidgetReceiver>): GlanceAppWidget

    /**
     * Provide a state to use for the [androidx.glance.state.GlanceStateDefinition] when composing
     * the provided [GlanceAppWidget] instance.
     *
     * @param instance - The [GlanceAppWidget] instance that will be used to compose the preview
     */
    open suspend fun getGlanceState(instance: GlanceAppWidget): Any? = null

    private val glanceRemoteViews = GlanceRemoteViews()

    /**
     * Only override this method to directly provide [RemoteViews] instead of [GlanceAppWidget]
     * instances.
     *
     * @see AppWidgetPreviewActivity.getAppWidgetPreview
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun getAppWidgetPreview(
        info: AppWidgetProviderInfo,
        size: DpSize
    ): RemoteViews {
        val receiver = Class.forName(info.provider.className)
        require(GlanceAppWidgetReceiver::class.java.isAssignableFrom(receiver)) {
            "AppWidget is not a GlanceAppWidgetReceiver. Override this method to provide other implementations"
        }

        val receiverClass = receiver as Class<out GlanceAppWidgetReceiver>
        return getGlancePreview(receiverClass).asRemoteViews(info, size)
    }

    private suspend fun GlanceAppWidget.asRemoteViews(
        info: AppWidgetProviderInfo,
        availableSize: DpSize
    ): RemoteViews = withContext(Dispatchers.IO) {
        val state = getGlanceState(this@asRemoteViews)
        when (val mode = sizeMode) {
            SizeMode.Single -> compose(info.getSingleSize(applicationContext), state)
            SizeMode.Exact -> compose(availableSize, state)
            is SizeMode.Responsive -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                composeResponsive(mode.sizes, state)
            } else {
                compose(findBestSize(availableSize, mode.sizes), state)
            }
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
