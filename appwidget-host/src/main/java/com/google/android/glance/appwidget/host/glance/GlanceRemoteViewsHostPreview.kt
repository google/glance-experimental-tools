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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidget
import com.google.android.glance.appwidget.host.AppWidgetHost
import com.google.android.glance.appwidget.host.rememberAppWidgetHostState

/**
 * Use this composable inside a [androidx.compose.ui.tooling.preview.Preview] composable to
 * display a glanceable composable
 *
 * @param modifier defines the container box for the host
 * @param state the state associated to the composable as per [GlanceAppWidget.stateDefinition]
 * @param displaySize the available size for the RemoteViews, if not provider it will match parent
 * @param provider optionally provide the [AppWidgetProviderInfo] to provide additional information
 * for the host.
 * @param content a suspend lambda returning the actual RemoteViews
 */
@ExperimentalGlanceRemoteViewsApi
@Composable
fun GlanceRemoteViewsHostPreview(
    glanceAppWidget: GlanceAppWidget,
    modifier: Modifier = Modifier,
    state: Any? = null,
    displaySize: DpSize = DpSize.Unspecified,
    provider: AppWidgetProviderInfo? = null
) {
    val hostState = rememberAppWidgetHostState(provider)
    val context = LocalContext.current
    if (hostState.isReady) {
        LaunchedEffect(hostState.value) {
            hostState.updateAppWidget(
                glanceAppWidget.compose(
                    context = context,
                    size = displaySize,
                    state = state,
                )
            )
        }
    }
    AppWidgetHost(modifier, displaySize, hostState)
}
