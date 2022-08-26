/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.android.glance.tools.viewer.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.android.glance.appwidget.host.AppWidgetHost
import com.google.android.glance.appwidget.host.AppWidgetHostState
import com.google.android.glance.appwidget.host.exportSnapshot
import com.google.android.glance.appwidget.host.rememberAppWidgetHostState
import com.google.android.glance.appwidget.host.requestPin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
internal fun ViewerScreen(
    providers: List<AppWidgetProviderInfo>,
    selectedProvider: AppWidgetProviderInfo,
    currentSize: DpSize,
    snapshot: suspend (AppWidgetProviderInfo, DpSize) -> RemoteViews,
    onResize: (DpSize) -> Unit,
    onSelected: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    var viewerPanelState by remember { mutableStateOf(ViewerPanel.Resize) }
    val snapshotHostState = rememberAppWidgetHostState(selectedProvider)

    if (snapshotHostState.isReady) {
        LaunchedEffect(snapshotHostState.value) {
            // Show first the initial layout
            snapshotHostState.updateAppWidget(
                RemoteViews(context.packageName, selectedProvider.initialLayout)
            )

            // Then request the actual snapshot
            snapshotHostState.updateAppWidget(
                snapshot(selectedProvider, currentSize)
            )
        }
        if (viewerPanelState == ViewerPanel.Resize && bottomSheetState.isVisible) {
            LaunchedEffect(currentSize) {
                // If the user is in resizing mode debounce the updates by delaying them.
                delay(500)
                snapshotHostState.updateAppWidget(
                    snapshot(selectedProvider, currentSize)
                )
            }
        }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
        sheetShape = MaterialTheme.shapes.large.copy(
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp)
        ),
        sheetContent = {
            when (viewerPanelState) {
                ViewerPanel.Resize -> ViewerResizePanel(
                    currentSize = currentSize,
                    onSizeChange = onResize
                )
                ViewerPanel.Info -> ViewerInfoPanel(selectedProvider)
            }
        }
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ViewerDrawer(providers, selectedProvider, drawerState, onSelected)
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                bottomBar = {
                    ViewerBottomBar(
                        drawerState = drawerState,
                        appWidgetHostState = snapshotHostState,
                        onUpdate = {
                            scope.launch {
                                snapshotHostState.updateAppWidget(
                                    snapshot(selectedProvider, currentSize)
                                )
                            }
                        },
                        onShowPanel = {
                            viewerPanelState = it
                            scope.launch { bottomSheetState.show() }
                        },
                        onPin = {
                            scope.launch {
                                val requested = snapshotHostState.requestPin()
                                if (!requested) {
                                    snackbarHostState.showSnackbar(
                                        message = "Launcher does not support AppWidget pinning.",
                                        withDismissAction = true
                                    )
                                }
                            }
                        },
                        onExport = {
                            scope.launch {
                                doExport(
                                    context,
                                    snackbarHostState,
                                    snapshotHostState,
                                )
                            }
                        }
                    )
                },
            ) { innerPadding ->
                AppWidgetHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    displaySize = currentSize,
                    state = snapshotHostState,
                    gridColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

private suspend fun doExport(
    context: Context,
    snackbarHostState: SnackbarHostState,
    appWidgetHostState: AppWidgetHostState,
) {
    val host = appWidgetHostState.value ?: return
    val uriResult = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        Result.failure(UnsupportedOperationException("Export only support from Android 10"))
    } else {
        host.exportSnapshot()
    }

    if (uriResult.isFailure) {
        snackbarHostState.showSnackbar(
            message = uriResult.exceptionOrNull()?.message ?: "Something went wrong",
            withDismissAction = true
        )
    } else {
        val result = snackbarHostState.showSnackbar(
            message = "Snapshot stored in gallery",
            actionLabel = "View",
            withDismissAction = true
        )
        if (result == SnackbarResult.ActionPerformed) {
            val intent = Intent(Intent.ACTION_VIEW, uriResult.getOrThrow()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerBottomBar(
    drawerState: DrawerState,
    appWidgetHostState: AppWidgetHostState,
    onUpdate: () -> Unit,
    onShowPanel: (ViewerPanel) -> Unit,
    onPin: () -> Unit,
    onExport: () -> Unit
) {
    val scope = rememberCoroutineScope()
    BottomAppBar(
        actions = {
            IconButton(onClick = {
                scope.launch {
                    if (drawerState.isClosed) {
                        drawerState.open()
                    } else {
                        drawerState.close()
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = null
                )
            }
            IconButton(
                enabled = appWidgetHostState.isReady,
                onClick = {
                    onShowPanel(ViewerPanel.Resize)
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null
                )
            }
            IconButton(
                enabled = appWidgetHostState.isReady,
                onClick = {
                    onShowPanel(ViewerPanel.Info)
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null
                )
            }
            IconButton(
                enabled = appWidgetHostState.isReady,
                onClick = {
                    onUpdate()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null
                )
            }
            IconButton(
                enabled = appWidgetHostState.isReady,
                onClick = {
                    onPin()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = null
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onExport()
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Export current snapshot"
                )
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ViewerDrawer(
    providers: List<AppWidgetProviderInfo>,
    selectedProvider: AppWidgetProviderInfo,
    drawerState: DrawerState,
    onSelected: (AppWidgetProviderInfo) -> Unit
) {
    val scope = rememberCoroutineScope()
    ModalDrawerSheet {
        Text(
            "Select widget",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        providers.forEach { item ->
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = if (selectedProvider == item) {
                            Icons.Rounded.Done
                        } else {
                            Icons.Rounded.KeyboardArrowRight
                        },
                        contentDescription = null
                    )
                },
                label = { Text(item.loadLabel(LocalContext.current.packageManager)) },
                selected = item == selectedProvider,
                onClick = {
                    scope.launch { drawerState.close() }
                    onSelected(item)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}
