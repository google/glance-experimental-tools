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

import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.glance.appwidget.host.getTargetSize

@Composable
internal fun PreviewInfoPanel(providerInfo: AppWidgetProviderInfo) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        InfoText(key = "Label", value = providerInfo.loadLabel(context.packageManager))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val description = providerInfo.loadDescription(context)?.toString()
            InfoText(
                key = "Description",
                value = description ?: "Missing description tag",
                isError = description.isNullOrBlank()
            )
        } else {
            InfoText(key = "Description", value = "Device does not support it", isError = true)
        }

        val hasInitialLayout = providerInfo.initialLayout != 0
        InfoText(
            key = "Initial layout",
            value = if (hasInitialLayout) "Available" else "Missing initial layout",
            isError = !hasInitialLayout
        )

        InfoText(
            key = "Default Size",
            value = providerInfo.getTargetSize(context).toString()
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val targetSizeAvailable = providerInfo.targetCellWidth > 0
            InfoText(
                key = "Target Cells",
                value = if (targetSizeAvailable) {
                    "${providerInfo.targetCellWidth}x${providerInfo.targetCellHeight}"
                } else {
                    "Missing target cells.\nminWidth/Height will be used instead"
                },
                isError = !targetSizeAvailable
            )
        }

        if (providerInfo.resizeMode == AppWidgetProviderInfo.RESIZE_NONE) {
            InfoText(
                key = "Resizeable",
                value = "Widget is not resizable",
                isError = true
            )
        } else {
            val hasResizeWidth = providerInfo.minResizeWidth != providerInfo.minWidth
            InfoText(
                key = "minResizeWidth",
                value = if (hasResizeWidth) "${providerInfo.minResizeWidth}" else "No MIN resize width provided.\nminWidth will be used instead",
                isError = !hasResizeWidth
            )

            val hasResizeHeight = providerInfo.minResizeHeight != providerInfo.minHeight
            InfoText(
                key = "minResizeHeight",
                value = if (hasResizeHeight) "${providerInfo.minResizeHeight}" else "No MIN resize height provided.\nminHeight will be used instead",
                isError = !hasResizeHeight
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasMaxResizeWidth = providerInfo.maxResizeWidth > 0
                InfoText(
                    key = "maxResizeWidth",
                    value = if (hasMaxResizeWidth) "${providerInfo.maxResizeWidth}" else "Missing",
                    isError = !hasMaxResizeWidth
                )
                val hasMaxResizeHeight = providerInfo.maxResizeHeight > 0
                InfoText(
                    key = "maxResizeHeight",
                    value = if (hasMaxResizeHeight) "${providerInfo.maxResizeHeight}" else "Missing",
                    isError = !hasResizeHeight
                )
            }
        }

        val hasPreviewImage = providerInfo.previewImage != 0
        InfoText(
            key = "previewImage",
            value = if (hasPreviewImage) "Available" else "Missing. App Icon will be used instead",
            isError = !hasPreviewImage
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPreviewLayout = providerInfo.previewLayout != 0
            InfoText(
                key = "previewLayout",
                value = if (hasPreviewLayout) "Available" else "Missing. previewImage will be used instead",
                isError = !hasPreviewLayout
            )
        }
        if (providerInfo.configure != null) {
            InfoText(
                key = "Configuration",
                value = providerInfo.configure.className,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val isOptional =
                    providerInfo.widgetFeatures >= AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL
                InfoText(
                    key = "Configuration Type",
                    value = if (isOptional) "Optional" else "Mandatory",
                )
                val isReconfigurable =
                    providerInfo.widgetFeatures ==
                            AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE ||
                            providerInfo.widgetFeatures == AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE + AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL ||
                            providerInfo.widgetFeatures == AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE + AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL + AppWidgetProviderInfo.WIDGET_FEATURE_HIDE_FROM_PICKER
                InfoText(
                    key = "Reconfigurable",
                    value = if (isReconfigurable) "Yes" else "No",
                    isError = !isReconfigurable
                )
            }
        }
    }
}

@Composable
private fun InfoText(key: String, value: String, isError: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$key:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.End
        )
    }
}