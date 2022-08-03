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

package com.google.android.glance.appwidget.configuration

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.GlanceStateDefinition
import com.google.android.glance.appwidget.host.AppWidgetHost
import com.google.android.glance.appwidget.host.AppWidgetHostState
import kotlinx.coroutines.launch

class AppWidgetConfigurationState(val instance: GlanceAppWidget) {

    var glanceId: GlanceId? by mutableStateOf(null)
        internal set

    var state: Any? by mutableStateOf(null)

    inline fun <reified T> updatePreviewState(update: (T) -> T) {
        requireNotNull(state)
        state = update(state as T)
    }
}

@Composable
fun rememberAppWidgetConfigurationState(instance: GlanceAppWidget) = remember(instance) {
    AppWidgetConfigurationState(instance)
}

@ExperimentalGlanceRemoteViewsApi
@ExperimentalMaterial3Api
@Composable
fun AppWidgetConfigurationScaffold(
    appWidgetConfigurationState: AppWidgetConfigurationState,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    previewColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    displaySize: DpSize = DpSize.Unspecified,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable ((Boolean) -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity
    activity.setResult(Activity.RESULT_CANCELED)

    val manager = GlanceAppWidgetManager(activity)
    val glanceId = manager.getGlanceIdBy(activity.intent).also {
        appWidgetConfigurationState.glanceId = it
    }
    if (glanceId == null) {
        activity.finish()
        return
    }

    LaunchedEffect(glanceId) {
        appWidgetConfigurationState.state = getAppWidgetState(
            context = activity,
            definition = appWidgetConfigurationState.instance.stateDefinition as GlanceStateDefinition<*>,
            glanceId = glanceId
        )
    }

    val orientation = LocalConfiguration.current.orientation
    var widgetSize by remember {
        mutableStateOf(displaySize)
    }

    // If no display size specified get the launcher available size
    if (displaySize == DpSize.Unspecified) {
        LaunchedEffect(widgetSize, appWidgetConfigurationState.glanceId, orientation) {
            val sizes = manager.getAppWidgetSizes(appWidgetConfigurationState.glanceId!!)
            widgetSize = when (orientation) {
                Configuration.ORIENTATION_PORTRAIT -> sizes.first()
                Configuration.ORIENTATION_LANDSCAPE -> sizes.getOrElse(1) {
                    // Edge case: only one size available. Swap size values
                    val portraitSize = sizes.first()
                    portraitSize.copy(width = portraitSize.height, height = portraitSize.width)
                }
                else -> throw IllegalArgumentException("Unknown orientation value")
            }
        }
    }

    val remoteViews = remember {
        GlanceRemoteViews()
    }
    val previewState = remember {
        AppWidgetHostState(mutableStateOf(null))
    }
    if (previewState.isReady && widgetSize != DpSize.Unspecified) {
        LaunchedEffect(
            previewState,
            appWidgetConfigurationState.state,
            appWidgetConfigurationState.instance,
            widgetSize
        ) {
            previewState.updatePreview(
                remoteViews.compose(
                    context = activity,
                    size = widgetSize,
                    state = appWidgetConfigurationState.state,
                    content = {
                        appWidgetConfigurationState.instance.Content()
                    }
                ).remoteViews
            )
        }
    }

    Scaffold(
        modifier,
        topBar,
        bottomBar,
        snackbarHost,
        floatingActionButton,
        floatingActionButtonPosition,
        containerColor,
        contentColor,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            AppWidgetHost(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(previewColor)
                    .padding(16.dp),
                widgetSize = widgetSize,
                state = previewState,
                showGrid = false
            )
            content { configured ->
                if (configured) {
                    activity.setResult(Activity.RESULT_OK, activity.intent)
                    scope.launch {
                        @Suppress("UNCHECKED_CAST")
                        updateAppWidgetState(
                            activity,
                            appWidgetConfigurationState.instance.stateDefinition as GlanceStateDefinition<Any?>,
                            glanceId
                        ) {
                            appWidgetConfigurationState.state
                        }
                        appWidgetConfigurationState.instance.update(activity, glanceId)
                    }
                }
                activity.finish()
            }
        }
    }
}