# AppWidget Host composable

[![Maven Central](https://img.shields.io/maven-central/v/com.google.android.glance.tools/appwidget-host)](https://search.maven.org/search?q=g:com.google.android.glance.tools)

A simple composable to display RemoteViews inside your app or to create `@Preview`s that together 
with Compose and [Live Edits](https://developer.android.com/jetpack/compose/tooling#live-edit)
enables, [in most situations](https://developer.android.com/studio/run#limitations), a real-time
update mechanism, reflecting code changes nearly instantaneously.

> Note: This library is used by the appwidget-viewer and appwidget-configuration modules and is
> independent from Glance-appwidget but provides extensions when glance-appwidget dependency is
> present in the project

## Setup

```groovy
repositories {
    mavenCentral()
}

dependencies {
    // or debugImplementation if only used for previews
    implementation "com.google.android.glance.tools:appwidget-host:<version>"
}
```

## Usage

Add the `AppWidgetHost` inside your UI by providing the available size for the AppWidget and the
`AppWidgetHostState` to interact with the host.

You can monitor the `isReady` value to then provide the RemoteViews to display in the host.

```kotlin
@Composable
fun MyScreen(provider: AppWidgetProviderInfo) {
    val state = rememberAppWidgetHostState(provider)
    if (previewHostState.isReady) {
        previewHostState.updateAppWidget(
            // Provide your RemoteViews
        )
    }
    AppWidgetHost(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        widgetSize = DpSize(200.dp, 200.dp),
        state = state
    )
}
```

### Use for Previews

The `AppWidgetHostPreview` enables [Jetpack Compose Live Previews](https://developer.android.com/jetpack/compose/tooling)
by creating a `@Preview`composable and running it in a device.

> Note: while the preview will render in Android Studio, the RemoteViews won't. You must always
> deploy them in a device ([guide](https://developer.android.com/jetpack/compose/tooling#preview-deploy)).

```kotlin
@Preview
@Composable
fun MyAppWidgetPreview() {
    // The size of the widget
    val displaySize = DpSize(200.dp, 200.dp)
    
    AppWidgetHostPreview(
        modifier = Modifier.fillMaxSize(),
        displaySize = displaySize
    ) { context ->
        RemoteViews(context.packageName, R.layout.my_widget_layout)
    }
}
```

If you use Glance for appwidget instead, the library provides an extension composable to simplify the setup

```kotlin
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Preview
@Composable
fun MyGlanceWidgetPreview() {
    // The size of the widget
    val displaySize = DpSize(200.dp, 200.dp)
    // Your GlanceAppWidget instance
    val instance = SampleGlanceWidget
    // Provide a state depending on the GlanceAppWidget state definition
    val state = preferencesOf(SampleGlanceWidget.countKey to 2)

    GlanceAppWidgetHostPreview(
        modifier = Modifier.fillMaxSize(),
        glanceAppWidget = instance,
        state = state,
        displaySize = displaySize,
    )
}
```

> Important: don't forget to setup the compose-tooling as explained [here](https://developer.android.com/jetpack/compose/tooling)

### Utils

The library also provides a set of common utils when working with AppWidgets and/or Glance:

- [AppWidgetHostUtils](src/main/java/com/google/android/glance/appwidget/host/AppWidgetHostUtils.kt)
- [AppWidgetSizeUtils](src/main/java/com/google/android/glance/appwidget/host/AppWidgetSizeUtils.kt)

## Snapshots

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].
These are updated on every commit.

[snap]: https://oss.sonatype.org/content/repositories/snapshots/com/google/android/glance/tools/appwidget-host/