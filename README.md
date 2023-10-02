# Glance Experimental Tools

> 🚧 Work in-progress: this library is under heavy development, APIs might change frequently

This project aims to supplement Jetpack Glance with features that are commonly required by
developers
but not yet available.

It is a labs like environment for Glance tooling. We use it to help fill known gaps in the
framework,
experiment with new APIs and to gather insight into the development experience of developing a
Glance library.

## Libraries

### 🧬️ [appwidget-host](./appwidget-host)

A simple composable to display RemoteViews inside your app or in `@Preview`s enabling
"[Live Edits](https://developer.android.com/studio/run#live-edit)" or
"[Apply Changes](https://developer.android.com/studio/run#apply-changes)".

### 🖼️ [appwidget-viewer](./appwidget-viewer)

A debug tool to view and interact with AppWidget snapshots embedded inside the app.

### 🖼️ [appwidget-testing](./appwidget-testing)

An activity to host a Glance composable for screenshot testing, without binding the entire
appWidget.

### 🛠️🎨 [appwidget-configuration](./appwidget-configuration)

A Material3 Scaffold implementation for appwidget configuration activities.

## Future?

Any of the features available in this group of libraries may become obsolete in the future, at which
point they will (probably) become deprecated.

We will aim to provide a migration path (where possible), to whatever supersedes the functionality.

## Why a separate repository?

We want to iterate, explore and experiment with some new APIs and tooling more freely, without
adding overhead to the main API and avoid API commitments. In addition, some of these features might
not be allowed in AndroidX.

> Note: this repository follows the [Accompanist](https://github.com/google/accompanist) pattern but
> in a much more narrow scope (read more about the idea behind this pattern
> [here](https://medium.com/androiddevelopers/jetpack-compose-accompanist-an-faq-b55117b02712))

## Contributions

Please contribute! We will gladly review any pull requests.
Make sure to read the [Contributing](CONTRIBUTING.md) page first though.

## License

```
Copyright 2020 The Android Open Source Project
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

