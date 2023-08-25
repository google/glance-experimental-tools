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

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.CallSuper
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
import com.google.android.glance.appwidget.host.getTargetSize
import com.google.android.glance.appwidget.host.glance.compose
import com.google.android.glance.tools.viewer.ui.ViewerScreen
import com.google.android.glance.tools.viewer.ui.theme.ViewerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base class to display AppWidgets.
 */
public abstract class AppWidgetViewerActivity : ComponentActivity() {

    /**
     * The list of [AppWidgetProvider] to display in the viewer
     */
    public abstract fun getProviders(): List<Class<out AppWidgetProvider>>

    /**
     * Provides the [RemoteViews] snapshot of the given [AppWidgetProviderInfo] for the given size
     *
     * @param - The [AppWidgetProviderInfo] containing the metadata of the appwidget
     * @param - The available size to display the appwidget
     *
     * @return the [RemoteViews] instance to use for the viewer.
     */
    public abstract suspend fun getAppWidgetSnapshot(
        info: AppWidgetProviderInfo,
        size: DpSize
    ): RemoteViews

    // Moving states outside of composition to ensure they are kept when Live Edits happen.
    private lateinit var selectedProvider: MutableState<AppWidgetProviderInfo>
    private lateinit var currentSize: MutableState<DpSize>

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetManager = AppWidgetManager.getInstance(this)
        val providers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            widgetManager.getInstalledProvidersForPackage(packageName, null)
        } else {
            widgetManager.installedProviders.filter { it.provider.packageName == packageName }
        }.filter { info ->
            getProviders().any { selectedProvider ->
                selectedProvider.name == info.provider.className
            }
        }

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
                        onResize = { currentSize.value = it },
                        onSelected = { selectedProvider.value = it }
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
public abstract class GlanceViewerActivity : AppWidgetViewerActivity() {

    /**
     * Provides an instance of [GlanceAppWidget] to display inside the viewer.
     *
     * @param receiver - The selected [GlanceAppWidgetReceiver] to display
     */
    public abstract suspend fun getGlanceSnapshot(receiver: Class<out GlanceAppWidgetReceiver>): GlanceSnapshot

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
        snapshot.instance.compose(applicationContext, size, snapshot.state, info)
    }
}
