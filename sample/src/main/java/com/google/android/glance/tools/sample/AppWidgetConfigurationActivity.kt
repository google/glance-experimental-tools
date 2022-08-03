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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import com.google.android.glance.appwidget.configuration.AppWidgetConfigurationScaffold
import com.google.android.glance.appwidget.configuration.AppWidgetConfigurationState
import com.google.android.glance.appwidget.configuration.rememberAppWidgetConfigurationState
import kotlinx.coroutines.launch

class AppWidgetConfigurationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SampleTheme {
                SampleConfigScreen()
            }
        }
    }

    @Composable
    private fun SampleTheme(content: @Composable () -> Unit) {
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
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = darkTheme
                }
            }
        }

        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlanceRemoteViewsApi::class)
@Composable
private fun SampleConfigScreen() {
    val configurationState = rememberAppWidgetConfigurationState(SampleGlanceWidget)
    val scope = rememberCoroutineScope()

    AppWidgetConfigurationScaffold(
        appWidgetConfigurationState = configurationState,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    configurationState.applyConfiguration()
                }
            }) {
                Icon(imageVector = Icons.Rounded.Done, contentDescription = "Save changes")
            }
        }
    ) {
        ConfigurationList(Modifier.padding(it), configurationState)
    }
}

@Composable
private fun ConfigurationList(modifier: Modifier, state: AppWidgetConfigurationState) {
    fun <T> updatePreferences(key: Preferences.Key<T>, value: T) {
        state.updateCurrentState<Preferences> {
            it.toMutablePreferences().apply {
                set(key, value)
            }.toPreferences()
        }
    }

    val counter = state.getCurrentState<Preferences>()?.get(SampleGlanceWidget.countKey) ?: 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Setup counter:")
        IconButton(onClick = {
            updatePreferences(SampleGlanceWidget.countKey, counter + 1)
        }) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add")
        }
        Text("$counter")
        IconButton(onClick = {
            updatePreferences(SampleGlanceWidget.countKey, counter - 1)
        }) {
            Icon(imageVector = Icons.Rounded.Remove, contentDescription = "Subtract")
        }
    }
}
