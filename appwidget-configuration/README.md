# GlanceAppWidget Configuration composable

[![Maven Central](https://img.shields.io/maven-central/v/com.google.android.glance.tools/appwidget-configuration)](https://search.maven.org/search?q=g:com.google.android.glance.tools)

A composable that uses Material3 Scaffold to display and handle the appwidget activity configuration
logic for Glance.

## Setup

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "com.google.android.glance.tools:appwidget-configuration:<version>"
}
```

## Usage

Follow the ["Declare the configuration activity"](https://developer.android.com/guide/topics/appwidgets/configuration#declare)
step in the official guidance to create and register the activity with the Material3 theme of your
choice:

```kotlin
class MyConfigurationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyMaterial3Theme {
                // ...
            }
        }
    }
}
```

Then, simply add the `AppWidgetConfigurationScaffold` composable with your configuration content UI
and provide the configuration state with the `GlanceAppWidget` instance to use for the preview.

```kotlin
val configurationState = rememberAppWidgetConfigurationState(MyGlanceWidget)
AppWidgetConfigurationScaffold(configurationState) { configured ->
    MyConfigurationList(
        onChange = { key, value ->
            configurationState.updatePreviewState<Preferences> {
                it.toMutablePreferences().apply {
                    set(key, value)
                }.toPreferences()
            }
        },
        configured = configured
    )
}
```

Call the `updatePreviewState` method to change the widget preview state and reflect the changes.

Once the configuration is completed call the provided `configured` lambda with true to apply the
changes to the widget or false to discard them.

Check the [TestConfigurationActivity](../sample/src/main/java/com/google/android/glance/tools/sample/TestConfigurationActivity.kt)
sample for more information.

## Snapshots

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].
These are updated on every commit.

[snap]: https://oss.sonatype.org/content/repositories/snapshots/com/google/android/glance/tools/appwidget-host/