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

package com.google.android.glance.tools.testing

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.preferencesOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.android.glance.appwidget.testing.GlanceScreenshotTestActivity
import com.google.android.glance.tools.sample.SampleGlanceWidget
import com.google.android.glance.tools.sample.SampleGlanceWidgetContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Pixel6)
class SampleGlanceScreenshotTest {

    @get:Rule
    val activityScenarioRule =
        ActivityScenarioRule(GlanceScreenshotTestActivity::class.java)

    @Test
    fun sampleGlanceContent() {
        renderComposable()

        captureAndVerifyScreenshot("sample_content")
    }

    @Test
    @Config(qualifiers = "+ar-ldrtl")
    fun sampleGlanceContent_rtl() {
        renderComposable()

        captureAndVerifyScreenshot("sample_content_rtl")
    }

    private fun renderComposable(size: DpSize = TEST_SIZE) {
        activityScenarioRule.scenario.onActivity {
            it.setAppWidgetSize(size)
            it.setState(preferencesOf(SampleGlanceWidget.countKey to 2))

            it.renderComposable {
                SampleGlanceWidgetContent()
            }
        }
    }

    companion object {
        private val TEST_SIZE = DpSize(300.dp, 200.dp)

        private fun captureAndVerifyScreenshot(goldenFileName: String) {
            onView(ViewMatchers.isRoot())
                .captureRoboImage(
                    filePath = "src/test/resources/golden/$goldenFileName.png",
                    roborazziOptions = RoborazziOptions(
                        compareOptions = RoborazziOptions.CompareOptions(
                            changeThreshold = 0F
                        )
                    )
                )
        }
    }
}
