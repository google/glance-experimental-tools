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

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import com.google.android.glance.appwidget.configuration.AppWidgetConfigurationScaffold
import com.google.android.glance.appwidget.configuration.rememberAppWidgetConfigurationState

class TestConfigurationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestTheme {
                TestConfigScreen()
            }
        }
    }

    @Composable
    private fun TestTheme(content: @Composable () -> Unit) {
        val darkTheme = isSystemInDarkTheme()
        val colorScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> darkColorScheme()
            else -> lightColorScheme()
        }
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                    darkTheme
            }
        }

        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlanceRemoteViewsApi::class)
@Composable
private fun TestConfigScreen() {
    val configurationState = rememberAppWidgetConfigurationState(TestGlanceWidget)

    AppWidgetConfigurationScaffold(appWidgetConfigurationState = configurationState) { configured ->
        LazyColumn {
            item {
                OutlinedButton(onClick = {
                    configurationState.updatePreviewState<Preferences> {
                        it.toMutablePreferences().apply {
                            set(TestGlanceWidget.countKey, System.currentTimeMillis())
                        }.toPreferences()
                    }
                }) {
                    Text(text = "Count")
                }
            }
            item {
                OutlinedButton(onClick = {
                    configured(true)
                }) {
                    Text(text = "Done")
                }
            }
        }
    }
}