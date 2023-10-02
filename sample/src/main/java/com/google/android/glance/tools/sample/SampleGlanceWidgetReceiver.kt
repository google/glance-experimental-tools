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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.google.android.glance.appwidget.host.glance.GlanceAppWidgetHostPreview

class SampleGlanceWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = SampleGlanceWidget
}

object SampleGlanceWidget : GlanceAppWidget() {

    val countKey = intPreferencesKey("count")

    override val sizeMode: SizeMode = SizeMode.Exact

    @Composable
    override fun Content() {
        SampleGlanceWidgetContent()
    }
}

@Composable
fun SampleGlanceWidgetContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.White)
            .cornerRadius(16.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val count = currentState(SampleGlanceWidget.countKey) ?: 0
        val size = LocalSize.current

        CountRow(count)
        SizeText(size)
    }
}

@Composable
fun CountRow(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.ic_android),
            contentDescription = "android icon",
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = "Count: $count",
            style = TextStyle(
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline
            )
        )
    }
}

@Composable
fun SizeText(size: DpSize) {
    Text(
        text = "${size.width.value.toInt()} - ${size.height.value.toInt()}",
        style = TextStyle(textAlign = TextAlign.Center)
    )
}

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Preview
@Composable
fun SampleGlanceWidgetPreview() {
    // The size of the widget
    val displaySize = DpSize(200.dp, 200.dp)
    // Provide a state depending on the GlanceAppWidget state definition
    val state = preferencesOf(SampleGlanceWidget.countKey to 2)

    GlanceAppWidgetHostPreview(
        modifier = Modifier.fillMaxSize(),
        glanceAppWidget = SampleGlanceWidget,
        state = state,
        displaySize = displaySize,
    )
}
