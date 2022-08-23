# GlanceAppWidget Configuration composable

[![Maven Central](https://img.shields.io/maven-central/v/com.google.android.glance.tools/appwidget-configuration)](https://search.maven.org/search?q=g:com.google.android.glance.tools)

A composable that uses Material3 Scaffold to display and handle the appwidget activity configuration
logic for Glance.

<img src="images/glance-configuration-demo.gif" width = 279px>

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

Follow the
["Declare the configuration activity"](https://developer.android.com/guide/topics/appwidgets/configuration#declare)
step in the official guidance to create and register the activity with the Material3 theme of your
choice:

```kotlin
class MyConfigurationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyMaterial3Theme {
                ConfigurationScreen()
            }
        }
    }
}
```

Then, add the `AppWidgetConfigurationScaffold` composable with your configuration content UI
and provide the configuration state with the `GlanceAppWidget` instance to use for the preview.

```kotlin
@Composable
private fun ConfigurationScreen() {
    val scope = rememberCoroutineScope()
    val configurationState = rememberAppWidgetConfigurationState(SampleGlanceWidget)

    // If we don't have a valid id, discard configuration and finish the activity.
    if (configurationState.glanceId == null) {
        configurationState.discardConfiguration()
        return
    }

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
        // Add your configuration content
    }
}
```

Use the `AppWidgetConfigurationState` methods to update the `GlanceAppWidget` state shown in the
preview and to apply or discard the changes.

* `updateCurrentState<T>(update: (T) -> T)`: updates the `GlanceAppWidget` state for the configuration preview without modifying the actual state.
* `getCurrentState()`: get the current `GlanceAppWidget` state for the configuration preview (before calling `updateCurrentState` it will be the actual state).
* `applyConfiguration()`: updates the actual `GlanceAppWidget` instance state and finish the activity.
* `discardConfiguration()`: discards any state changes and finishes the activity with `RESULT_CANCELED`.

Check the
[AppWidgetConfigurationActivity](../sample/src/main/java/com/google/android/glance/tools/sample/AppWidgetConfigurationActivity.kt)
sample for more information.

## Snapshots

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].
These are updated on every commit.

[snap]: https://oss.sonatype.org/content/repositories/snapshots/com/google/android/glance/tools/appwidget-host/