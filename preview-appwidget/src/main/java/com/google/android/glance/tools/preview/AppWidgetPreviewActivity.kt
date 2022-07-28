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

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.google.android.glance.tools.preview.internal.AppWidgetPreviewManager
import com.google.android.glance.tools.preview.internal.getSingleSize
import com.google.android.glance.tools.preview.internal.getTargetSize
import com.google.android.glance.tools.preview.internal.toPixels
import com.google.android.glance.tools.preview.internal.toSizeF
import com.google.android.glance.tools.preview.internal.ui.PreviewScreen
import com.google.android.glance.tools.preview.internal.ui.theme.PreviewTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

abstract class AppWidgetPreviewActivity : ComponentActivity() {

    abstract fun getProviders(): List<Class<out AppWidgetProvider>>

    abstract suspend fun getAppWidgetPreview(
        info: AppWidgetProviderInfo,
        size: DpSize
    ): RemoteViews

    // Moving states outside of composition to ensure they are kept when Live Edits happen.
    private lateinit var selectedProvider: MutableState<AppWidgetProviderInfo>
    private lateinit var currentSize: MutableState<DpSize>

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
                        exportPreview = { previewManager.exportPreview(it) },
                        onResize = { currentSize.value = it },
                        onSelected = { selectedProvider.value = it }
                    )
                }
            }
        }
    }

    fun AppWidgetProviderInfo.extractOptions(availableSize: DpSize): Bundle {
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
                val context = this@AppWidgetPreviewActivity
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
}

@ExperimentalGlanceRemoteViewsApi
abstract class GlancePreviewActivity : AppWidgetPreviewActivity() {

    abstract suspend fun getGlancePreview(receiver: Class<out GlanceAppWidgetReceiver>): GlanceAppWidget

    open suspend fun getGlanceState(instance: GlanceAppWidget): Any? = null

    private val glanceRemoteViews = GlanceRemoteViews()

    override suspend fun getAppWidgetPreview(
        info: AppWidgetProviderInfo,
        size: DpSize
    ): RemoteViews {
        val receiver = Class.forName(info.provider.className)
        require(GlanceAppWidgetReceiver::class.java.isAssignableFrom(receiver)) {
            "AppWidget is not a GlanceAppWidgetReceiver. Override this method to provide other implementations"
        }

        @Suppress("UNCHECKED_CAST")
        return getGlancePreview(receiver as Class<out GlanceAppWidgetReceiver>).asRemoteViews(
            info = info,
            availableSize = size
        )
    }

    private suspend fun GlanceAppWidget.asRemoteViews(
        info: AppWidgetProviderInfo,
        availableSize: DpSize
    ): RemoteViews = withContext(Dispatchers.IO) {
        val state = getGlanceState(this@asRemoteViews)
        when (val mode = sizeMode) {
            SizeMode.Single -> {
                glanceRemoteViews.compose(
                    context = applicationContext,
                    size = info.getSingleSize(applicationContext),
                    state = state,
                    content = { Content() }
                ).remoteViews
            }
            SizeMode.Exact -> {
                glanceRemoteViews.compose(
                    context = applicationContext,
                    size = availableSize,
                    state = state,
                    content = { Content() }
                ).remoteViews
            }
            is SizeMode.Responsive -> {
                val allViews = mode.sizes.map { size ->
                    async {
                        size.toSizeF() to glanceRemoteViews.compose(
                            context = applicationContext,
                            size = size,
                            state = state,
                            content = { Content() }
                        ).remoteViews
                    }
                }.awaitAll()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    allViews.singleOrNull()?.second ?: RemoteViews(allViews.toMap())
                } else {
                    // TODO improve this or document that responsive mode only works properly on S+
                    allViews.singleOrNull()!!.second
                }
            }
        }
    }
}
