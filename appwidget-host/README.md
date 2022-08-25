# AppWidget Host composable

[![Maven Central](https://img.shields.io/maven-central/v/com.google.android.glance.tools/appwidget-host)](https://search.maven.org/search?q=g:com.google.android.glance.tools)

A simple composable to display RemoteViews inside your app or to create `@Preview`s that together 
with Compose and [Live Edits](https://developer.android.com/jetpack/compose/tooling#live-edit)
enables, [in most situations](https://developer.android.com/studio/run#limitations), a hot-reload
mechanism, reflecting changes nearly instantaneously.

> Note: This library is used by the appwidget-viewer and appwidget-configuration modules and is
> independent from Glance-appwidget

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

Note: while the preview will render in Android Studio, the RemoteViews won't. You must always run
it in a device.

When using Glance-appwidgets you can use the experimental API `GlanceRemoteViews` to generate the
RemoteViews to use for the preview

```kotlin
@Preview
@Composable
fun MyGlancePreview() {
    // Helper class to trigger composition in a Glance composable
    val remoteViews = GlanceRemoteViews()
    // The size of the widget
    val displaySize = DpSize(200.dp, 200.dp)
    // Provide a state depending on the GlanceAppWidget state definition
    val state = preferencesOf(counterKey to 2)
    
    AppWidgetHostPreview(
        modifier = Modifier.fillMaxSize(),
        displaySize = displaySize
    ) { context ->
        remoteViews.compose(context, displaySize, state) {
            MyGlanceContent() // Call the glance composable
        }.remoteViews
    }
}
```

Important: don't forget to setup the compose-tooling as explained [here](https://developer.android.com/jetpack/compose/tooling)

## Snapshots

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].
These are updated on every commit.

[snap]: https://oss.sonatype.org/content/repositories/snapshots/com/google/android/glance/tools/appwidget-host/