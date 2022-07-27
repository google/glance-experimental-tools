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

package com.google.android.glance.tools.preview.internal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
internal fun PreviewResizePanel(
    currentSize: DpSize,
    onSizeChange: (DpSize) -> Unit
) {
    // TODO probably we should get the real max available size from the layout one measured
    val configuration = LocalConfiguration.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Resize widget:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val padding = Modifier.padding(start = 16.dp, end = 16.dp)
        Text(
            text = "Width: ${currentSize.width.value.toInt()}dp",
            style = MaterialTheme.typography.labelMedium,
            modifier = padding
        )
        Slider(
            modifier = padding,
            value = currentSize.width.value,
            valueRange = 48f..configuration.screenWidthDp.toFloat(),
            onValueChange = {
                onSizeChange(currentSize.copy(width = it.dp))
            }
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Height: ${currentSize.height.value.toInt()}dp",
            style = MaterialTheme.typography.labelMedium,
            modifier = padding
        )
        Slider(
            modifier = padding,
            value = currentSize.height.value,
            valueRange = 48f..configuration.screenHeightDp.toFloat(),
            onValueChange = { onSizeChange(currentSize.copy(height = it.dp)) })
    }
}