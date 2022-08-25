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

package com.google.android.glance.tools.sample

import android.appwidget.AppWidgetProviderInfo
import android.widget.RemoteViews
import androidx.compose.ui.unit.DpSize
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.google.android.glance.tools.viewer.GlanceSnapshot
import com.google.android.glance.tools.viewer.GlanceViewerActivity

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class SampleViewerActivity : GlanceViewerActivity() {

    private var counter = 0

    override fun getProviders() = listOf(
        SampleGlanceWidgetReceiver::class.java,
        SampleAppWidgetReceiver::class.java
    )

    override suspend fun getGlanceSnapshot(
        receiver: Class<out GlanceAppWidgetReceiver>
    ): GlanceSnapshot {
        return when (receiver) {
            SampleGlanceWidgetReceiver::class.java -> GlanceSnapshot(
                instance = SampleGlanceWidget,
                state = mutablePreferencesOf(
                    SampleGlanceWidget.countKey to counter++
                )
            )
            else -> throw IllegalArgumentException()
        }
    }

    /**
     * To support non-glance widgets we can override this method and provide the RemoteViews directly
     */
    override suspend fun getAppWidgetSnapshot(
        info: AppWidgetProviderInfo,
        size: DpSize
    ): RemoteViews {
        return when (info.provider.className) {
            SampleAppWidgetReceiver::class.java.name -> SampleAppWidget.createWidget(this)
            else -> super.getAppWidgetSnapshot(info, size)
        }
    }
}
