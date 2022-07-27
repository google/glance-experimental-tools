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

package com.google.android.glance.tools.preview.internal.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val Context.widgetCornerRadiiPx: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        resources.getDimensionPixelSize(android.R.dimen.system_app_widget_background_radius)
    } else {
        (16 * resources.displayMetrics.density).toInt()
    }

internal val Context.widgetCornerRadiiDp: Dp
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val size =
            resources.getDimensionPixelSize(android.R.dimen.system_app_widget_background_radius)
        (size / resources.displayMetrics.density).dp
    } else {
        16.dp
    }