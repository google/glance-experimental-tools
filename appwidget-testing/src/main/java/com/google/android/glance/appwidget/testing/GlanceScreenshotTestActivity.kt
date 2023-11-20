/*
 * Copyright 2023 The Android Open Source Project
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

package com.google.android.glance.appwidget.testing

import android.app.Activity
import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import kotlinx.coroutines.runBlocking

/**
 * An activity that acts as a host for independently rendering glance composable content in
 * screenshot tests.
 *
 * See README.md for usage.
 */
@RequiresApi(Build.VERSION_CODES.O)
public class GlanceScreenshotTestActivity : Activity() {
    private var state: Any? = null
    private var size: DpSize = DpSize(Dp.Unspecified, Dp.Unspecified)
    private var wrapContentSize: Boolean = false
    private lateinit var hostView: AppWidgetHostView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_activity_layout)
    }

    /**
     * Sets the appwidget state that can be accessed via LocalState composition local.
     */
    public fun <T> setState(state: T) {
        this.state = state
    }

    /**
     * Sets the size of appwidget to be assumed for the test. This corresponds to the "LocalSize"
     * composition local.
     *
     * Content will be rendered in this size, unless wrapContentSize was set.
     */
    public fun setAppWidgetSize(size: DpSize) {
        this.size = size
    }

    /**
     * Sets the size of rendering area to wrap size of the composable under test instead of using
     * the same size as one provided in [setAppWidgetSize]. This is useful when you are testing a
     * small part of the appwidget independently.
     *
     * Note: Calling [wrapContentSize] doesn't impact "LocalSize" compositionLocal. Use
     * [setAppWidgetSize] to set the value that should be used for the compositionLocal.
     */
    public fun wrapContentSize() {
        this.wrapContentSize = true
    }

    /**
     * Renders the given glance composable in the activity.
     *
     * Provide appwidget size before calling this.
     */
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    public fun renderComposable(composable: @Composable () -> Unit) {
        runBlocking {
            val remoteViews = GlanceRemoteViews().compose(
                context = applicationContext,
                size = size,
                state = state,
                content = composable
            ).remoteViews

            val activityFrame = findViewById<FrameLayout>(R.id.content)
            hostView = TestHostView(applicationContext)
            hostView.setBackgroundColor(Color.WHITE)
            activityFrame.addView(hostView)

            val view = remoteViews.apply(applicationContext, hostView)
            hostView.addView(view)

            adjustHostViewSize()
        }
    }

    private fun adjustHostViewSize() {
        val displayMetrics = resources.displayMetrics

        if (wrapContentSize) {
            hostView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        } else {
            val hostViewPadding = Rect()
            val width =
                size.width.toPixels(displayMetrics) + hostViewPadding.left + hostViewPadding.right
            val height =
                size.height.toPixels(displayMetrics) + hostViewPadding.top + hostViewPadding.bottom

            hostView.layoutParams = FrameLayout.LayoutParams(width, height, Gravity.CENTER)
        }

        hostView.requestLayout()
    }

    private fun Dp.toPixels(displayMetrics: DisplayMetrics) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics).toInt()

    @RequiresApi(Build.VERSION_CODES.O)
    private class TestHostView(context: Context) : AppWidgetHostView(context) {
        init {
            // Prevent asynchronous inflation of the App Widget
            setExecutor(null)
            layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        }
    }
}
