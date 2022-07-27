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

package com.google.android.glance.tools.preview.internal.ui

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
internal fun PreviewScreen(
    providers: List<AppWidgetProviderInfo>,
    selectedProvider: AppWidgetProviderInfo,
    currentSize: DpSize,
    preview: suspend (AppWidgetProviderInfo, DpSize) -> RemoteViews,
    exportPreview: suspend (AppWidgetHostView) -> Result<Uri>,
    onResize: (DpSize) -> Unit,
    onSelected: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    var previewPanelState by remember { mutableStateOf(PreviewPanel.Resize) }
    val previewHostState = rememberWidgetHost(selectedProvider)

    if (previewHostState.isReady) {
        LaunchedEffect(previewHostState.value, currentSize) {
            // If the user is in resizing mode debounce the updates by delaying them.
            if (previewPanelState == PreviewPanel.Resize && bottomSheetState.isVisible) {
                delay(500)
            }
            previewHostState.updatePreview(preview(selectedProvider, currentSize))
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
            when (previewPanelState) {
                PreviewPanel.Resize -> PreviewResizePanel(
                    currentSize = currentSize,
                    onSizeChange = onResize
                )
                PreviewPanel.Info -> PreviewInfoPanel(selectedProvider)
            }
        }
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                PreviewDrawer(providers, selectedProvider, drawerState, onSelected)
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                bottomBar = {
                    PreviewBottomBar(
                        drawerState = drawerState,
                        previewHostState = previewHostState,
                        onUpdate = {
                            scope.launch {
                                previewHostState.updatePreview(
                                    preview(selectedProvider, currentSize)
                                )
                            }
                        },
                        onShowPanel = {
                            previewPanelState = it
                            scope.launch { bottomSheetState.show() }
                        },
                        onExport = {
                            scope.launch {
                                doExport(
                                    context,
                                    snackbarHostState,
                                    previewHostState,
                                    exportPreview
                                )
                            }
                        }
                    )
                },
            ) { innerPadding ->
                PreviewHost(
                    appWidgetInfo = selectedProvider,
                    widgetSize = currentSize,
                    previewState = previewHostState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                )
            }
        }
    }
}

private suspend fun doExport(
    context: Context,
    snackbarHostState: SnackbarHostState,
    previewHostState: PreviewHostState,
    exportPreview: suspend (AppWidgetHostView) -> Result<Uri>
) {
    val host = previewHostState.value ?: return
    val uriResult = exportPreview(host)
    if (uriResult.isFailure) {
        snackbarHostState.showSnackbar(
            message = uriResult.exceptionOrNull()?.message ?: "Something went wrong",
            withDismissAction = true
        )
    } else {
        val result = snackbarHostState.showSnackbar(
            message = "Preview stored in gallery",
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
private fun PreviewBottomBar(
    drawerState: DrawerState,
    previewHostState: PreviewHostState,
    onUpdate: () -> Unit,
    onShowPanel: (PreviewPanel) -> Unit,
    onExport: () -> Unit
) {
    val scope = rememberCoroutineScope()
    BottomAppBar(
        icons = {
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
                    imageVector = Icons.Filled.Menu,
                    contentDescription = null
                )
            }
            IconButton(
                enabled = previewHostState.isReady,
                onClick = {
                    onShowPanel(PreviewPanel.Resize)
                }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null
                )
            }
            IconButton(
                enabled = previewHostState.isReady,
                onClick = {
                    onShowPanel(PreviewPanel.Info)
                }) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null
                )
            }
            IconButton(
                enabled = previewHostState.isReady,
                onClick = {
                    onUpdate()
                }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
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
                    contentDescription = "Export preview"
                )
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PreviewDrawer(
    providers: List<AppWidgetProviderInfo>,
    selectedProvider: AppWidgetProviderInfo,
    drawerState: DrawerState,
    onSelected: (AppWidgetProviderInfo) -> Unit
) {
    val scope = rememberCoroutineScope()
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
                    }, contentDescription = null
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
