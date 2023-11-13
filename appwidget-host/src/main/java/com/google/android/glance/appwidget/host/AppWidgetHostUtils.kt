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

package com.google.android.glance.appwidget.host

import android.app.PendingIntent
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val SNAPSHOTS_FOLDER = "appwidget-snapshots"

/**
 * Request the launcher to pin the current hosted appwidget if any.
 *
 * @param target the provider component name or null to use the one defined in the host
 * @param successCallback a PendingIntent to send when the launcher successfully placed the widget
 * @return true if request was successful, false otherwise
 *
 * @see AppWidgetManager.requestPinAppWidget
 */
fun AppWidgetHostState.requestPin(
    target: ComponentName = value!!.appWidgetInfo.provider,
    successCallback: PendingIntent? = null
): Boolean {
    val widgetManager = AppWidgetManager.getInstance(value!!.context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && widgetManager.isRequestPinAppWidgetSupported) {
        val previewBundle = Bundle().apply {
            if (snapshot != null) {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, snapshot)
            }
        }
        return widgetManager.requestPinAppWidget(target, previewBundle, successCallback)
    }
    return false
}

/**
 * Extracts and image from the current host and stores it in the device picture directory
 *
 * @param fileName optional filename (without extension), otherwise the app name + date will be used.
 * @return the result of the operation with the image URI if successful
 */
@RequiresApi(Build.VERSION_CODES.Q)
suspend fun AppWidgetHostView.exportSnapshot(fileName: String? = null): Result<Uri> {
    return runCatching {
        withContext(Dispatchers.IO) {
            val bitmap = (this@exportSnapshot as View).toBitmap()
            val collection = MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
            val dirDest = File(Environment.DIRECTORY_PICTURES, SNAPSHOTS_FOLDER)
            val date = System.currentTimeMillis()
            val name = fileName ?: getSnapshotName(date)
            val newImage = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.DATE_ADDED, date)
                put(MediaStore.MediaColumns.DATE_MODIFIED, date)
                put(MediaStore.MediaColumns.SIZE, bitmap.byteCount)
                put(MediaStore.MediaColumns.WIDTH, bitmap.width)
                put(MediaStore.MediaColumns.HEIGHT, bitmap.height)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$dirDest${File.separator}")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            context.contentResolver.insert(collection, newImage)!!.apply {
                context.contentResolver.openOutputStream(this, "w").use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it!!)
                }
                newImage.clear()
                newImage.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(this, newImage, null, null)
            }
        }
    }
}

private fun AppWidgetHostView.getSnapshotName(date: Long) = (
    try {
        appWidgetInfo.loadLabel(context.packageManager)
    } catch (e: Exception) {
        Log.w("AppWidgetHostView", "Could not retrieve app label", e)
        null
    } ?: "unknown"
    ) + "-$date"

private fun View.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    layout(left, top, right, bottom)
    val clipPath = Path()
    val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
    val radi = context.appwidgetBackgroundRadiusPixels
    clipPath.addRoundRect(rect, radi, radi, Path.Direction.CW)
    canvas.clipPath(clipPath)
    draw(canvas)
    return bitmap
}
