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

Then, simply add the `AppWidgetConfigurationScaffold` composable with your configuration content UI
and provide the configuration state with the `GlanceAppWidget` instance to use for the preview.

```kotlin
@Composable
private fun ConfigurationScreen() {
    val configurationState = rememberAppWidgetConfigurationState(SampleGlanceWidget)
    val scope = rememberCoroutineScope()

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

Use the `AppWidgetConfigurationState` methods to modify the `GlanceAppWidget` state shown in the
preview and to apply or discard the changes.

Check the
[AppWidgetConfigurationActivity](../sample/src/main/java/com/google/android/glance/tools/sample/AppWidgetConfigurationActivity.kt)
sample for more information.

## Snapshots

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].
These are updated on every commit.

[snap]: https://oss.sonatype.org/content/repositories/snapshots/com/google/android/glance/tools/appwidget-host/