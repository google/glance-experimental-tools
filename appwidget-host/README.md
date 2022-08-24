# AppWidget Host composable

[![Maven Central](https://img.shields.io/maven-central/v/com.google.android.glance.tools/appwidget-host)](https://search.maven.org/search?q=g:com.google.android.glance.tools)

A simple composable to display RemoteViews inside your app, together with some AppWidget sizing
utilities.

> Note: This library is used by the appwidget-preview and appwidget-configuration modules and does
> not depend on Glance-appwidget

## Setup

```groovy
repositories {
    mavenCentral()
}

dependencies {
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

## Snapshots

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].
These are updated on every commit.

[snap]: https://oss.sonatype.org/content/repositories/snapshots/com/google/android/glance/tools/appwidget-host/