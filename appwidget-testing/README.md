# Glance AppWidget Testing

[![Maven Central](https://img.shields.io/maven-central/v/com.google.android.glance.tools/appwidget-testing)](https://search.maven.org/search?q=g:com.google.android.glance.tools)

## GlanceScreenshotTestActivity

A simple activity to render a Glance composable without binding an appwidget for screenshot testing.
It provides functions that can be called to initialize and render the Glance composable in it.

### Setup

```groovy
repositories {
    mavenCentral()
}

dependencies {
    debugImplementation "com.google.android.glance.tools:appwidget-testing:<version>"
}
```

### Usage

Define an `ActivityScenarioRule` in your test class for the `GlanceScreenshotTestActivity`.

```kotlin
    @get:Rule
    val activityScenarioRule =
        ActivityScenarioRule(GlanceScreenshotTestActivity::class.java)
```

Initialize the size and state for your composable in the `onActivity` runner and provide the
composable to be rendered in the `GlanceScreenshotTestActivity`.

```kotlin
activityScenarioRule.scenario.onActivity {
    it.setAppWidgetSize(size)
    it.setState(preferencesOf(myKey to 2))

    it.renderComposable {
        MyGlanceContent()
    }
}
```

Then, using screenshot testing tool of your choice capture and compare the screenshot of the
activity. For example, following sample uses [Roborazzi](https://github.com/takahirom/roborazzi)
capture and verify the screenshot.

```kotlin
Espresso.onView(ViewMatchers.isRoot())
    .captureRoboImage(
        filePath = "src/test/resources/golden/$goldenFileName.png",
        roborazziOptions = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(
                changeThreshold = 0F
            )
        )
    )
```

For a complete example, see
[SampleGlanceScreenshotTest](https://github.com/google/glance-experimental-tools/tree/main/sample/src/test/java/com/google/android/glance/tools/testing/SampleGlanceScreenshotTest.kt).
