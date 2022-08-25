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

package com.google.android.glance.tools.viewer.internal

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetProviderInfo
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.RemoteViews
import com.google.android.glance.appwidget.host.appwidgetBackgroundRadiusPixels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class AppWidgetViewerManager(
    private val context: Context,
    private val providers: List<Class<out AppWidgetProvider>>
) {

    companion object {
        private const val SNAPSHOTS_FOLDER = "appwidget-snapshots"
    }

    private val widgetManager = AppWidgetManager.getInstance(context)

    fun getProvidersInfo(): List<AppWidgetProviderInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            widgetManager.getInstalledProvidersForPackage(context.packageName, null)
        } else {
            widgetManager.installedProviders.filter { it.provider.packageName == context.packageName }
        }.filter { info ->
            providers.any { selectedProvider ->
                selectedProvider.name == info.provider.className
            }
        }
    }

    fun requestPin(info: AppWidgetProviderInfo, preview: RemoteViews): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && widgetManager.isRequestPinAppWidgetSupported) {
            val previewBundle = Bundle().apply {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview)
            }
            return widgetManager.requestPinAppWidget(info.provider, previewBundle, null)
        }
        return false
    }

    suspend fun exportSnapshot(
        info: AppWidgetProviderInfo,
        hostView: AppWidgetHostView
    ): Result<Uri> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Result.failure(
                UnsupportedOperationException("Export only support from Android 10")
            )
        }
        return runCatching {
            withContext(Dispatchers.IO) {
                val bitmap = loadBitmapFromView(hostView as View)
                val collection = MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
                val dirDest = File(Environment.DIRECTORY_PICTURES, SNAPSHOTS_FOLDER)
                val date = System.currentTimeMillis()
                val name = info.loadLabel(hostView.context.packageManager)
                val newImage = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$name-$date.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.DATE_ADDED, date)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, date)
                    put(MediaStore.MediaColumns.SIZE, bitmap.byteCount)
                    put(MediaStore.MediaColumns.WIDTH, bitmap.width)
                    put(MediaStore.MediaColumns.HEIGHT, bitmap.height)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$dirDest${File.separator}")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val newImageUri = context.contentResolver.insert(collection, newImage)
                context.contentResolver.openOutputStream(newImageUri!!, "w").use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                newImage.clear()
                newImage.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(newImageUri, newImage, null, null)
                newImageUri
            }
        }
    }

    private fun loadBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.width, view.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.layout(view.left, view.top, view.right, view.bottom)
        val clipPath = Path()
        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val radi = context.appwidgetBackgroundRadiusPixels
        clipPath.addRoundRect(rect, radi, radi, Path.Direction.CW)
        canvas.clipPath(clipPath)
        view.draw(canvas)
        return bitmap
    }
}
