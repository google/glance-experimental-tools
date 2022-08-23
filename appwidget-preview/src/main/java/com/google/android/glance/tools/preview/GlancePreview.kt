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

package com.google.android.glance.tools.preview

import androidx.glance.appwidget.GlanceAppWidget

/**
 * Data class that contains the instance to use for the preview and the associated state as defined
 * by the [GlanceAppWidget.stateDefinition] of the instance.
 */
data class GlancePreview(val instance: GlanceAppWidget, val state: Any? = null)
