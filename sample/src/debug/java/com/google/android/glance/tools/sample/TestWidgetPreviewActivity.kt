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
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.google.android.glance.tools.preview.GlancePreviewActivity

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class TestWidgetPreviewActivity : GlancePreviewActivity() {

    private var counter = 0

    /**
     * To support non-glance widgets we can override this method and provide the RemoteViews directly
     */
    override suspend fun getAppWidgetPreview(
        info: AppWidgetProviderInfo,
        size: DpSize
    ): RemoteViews {
        return when (info.provider.className) {
            TestAppWidgetReceiver::class.java.name -> TestAppWidget.createWidget(this)
            else -> super.getAppWidgetPreview(info, size)
        }
    }

    override suspend fun getGlancePreview(
        receiver: Class<out GlanceAppWidgetReceiver>
    ): GlanceAppWidget {
        return when (receiver) {
            TestGlanceWidgetReceiver::class.java -> TestGlanceWidget
            else -> throw IllegalArgumentException()
        }
    }

    override suspend fun getGlanceState(instance: GlanceAppWidget): Any {
        return when (instance) {
            is TestGlanceWidget -> mutablePreferencesOf(
                TestGlanceWidget.countKey to counter++
            )
            else -> throw IllegalArgumentException()
        }
    }

    override fun getProviders() = listOf(
        TestGlanceWidgetReceiver::class.java,
        TestAppWidgetReceiver::class.java
    )
}