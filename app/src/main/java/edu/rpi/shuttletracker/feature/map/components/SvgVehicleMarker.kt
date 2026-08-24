package edu.rpi.shuttletracker.feature.map.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.LruCache
import androidx.annotation.ColorInt
import androidx.core.graphics.PathParser
import androidx.core.graphics.createBitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlin.math.roundToInt

// Google Maps markers require a bitmap, so draw the bus icon directly onto a Canvas.

private fun buildVehicleMarkerBitmap(
    pxSize: Int,
    @ColorInt color: Int,
): Bitmap {
    val bitmap = createBitmap(pxSize, pxSize)
    val canvas = Canvas(bitmap)

    val paintOuter =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
    val paintInner =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = 0xFFFFFFFF.toInt()
        }
    val paintPath =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

    val viewBoxSize = 50f
    val centerPx = pxSize / 2f
    val scaleToPx = pxSize / viewBoxSize

    canvas.drawCircle(centerPx, centerPx, 25f * scaleToPx, paintOuter)
    canvas.drawCircle(centerPx, centerPx, 21f * scaleToPx, paintInner)

    val pathData =
        "M18 11H6V6h12m-1.5 11a1.5 1.5 0 0 1-1.5-1.5a1.5 1.5 0 0 1 1.5-1.5a1.5 1.5 0 0 1 1.5 1.5a1.5" +
            " 1.5 0 0 1-1.5 1.5m-9 0A1.5 1.5 0 0 1 6 15.5A1.5 1.5 0 0 1 7.5 14A1.5 1.5 0 0 1 9 15.5A1.5 1.5 0 0" +
            " 1 7.5 17M4 16c0 .88.39 1.67 1 2.22V20a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-1h8v1a1 1 0 0 0 1 1h1a1 1 0 0" +
            " 0 1-1v-1.78c.61-.55 1-1.34 1-2.22V6c0-3.5-3.58-4-8-4s-8 .5-8 4z"

    val path: Path = PathParser.createPathFromPathData(pathData)

    val svgMatrix =
        Matrix().apply {
            postTranslate(6f, 6f)
            postScale(1.6f, 1.6f, 6f, 6f)
        }
    path.transform(svgMatrix)

    val toPx =
        Matrix().apply {
            postScale(scaleToPx, scaleToPx)
        }
    path.transform(toPx)

    canvas.drawPath(path, paintPath)

    return bitmap
}

/** Reuses marker bitmaps by size and color. */
private object VehicleMarkerCache : LruCache<String, BitmapDescriptor>(8)

fun getVehicleMarkerDescriptor(
    context: Context,
    dpSize: Float,
    @ColorInt color: Int,
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val pxSize = (dpSize * density).roundToInt()

    val cacheKey = "pxSize=$pxSize;color=$color"
    VehicleMarkerCache.get(cacheKey)?.let { return it }

    val markerBitmap = buildVehicleMarkerBitmap(pxSize, color)
    val markerDescriptor = BitmapDescriptorFactory.fromBitmap(markerBitmap)

    VehicleMarkerCache.put(cacheKey, markerDescriptor)

    return markerDescriptor
}
