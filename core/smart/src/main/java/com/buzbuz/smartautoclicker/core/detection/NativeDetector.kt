
package com.buzbuz.smartautoclicker.core.detection

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.Keep
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Native implementation of the image detector.
 * It uses OpenCv template matching algorithms to achieve condition detection on the screen.
 *
 * Debug flavour of the library is build against build artifacts of OpenCv in the debug folder.
 * Release flavour of the library is build against the sources of the OpenCv project, downloaded from github.
 */
class NativeDetector private constructor() : ImageDetector {

    companion object {
        fun newInstance(): NativeDetector? = NativeDetector()
    }

    /** The results of the detection. Modified by native code. */
    @Keep
    private val detectionResult = DetectionResult()
    private val lock = Any()
    private var isClosed: Boolean = false
    private var screenDimensions: Point = Point(0, 0)
    private var screenBitmapCopy: Bitmap? = null
    private var cachedPixelsScreen: IntArray? = null
    private var cachedPixelsCondition: IntArray? = null

    override fun init() {
        isClosed = false
    }


    override fun close() = synchronized(lock) {
        if (isClosed) return

        isClosed = true
        screenBitmapCopy?.recycle()
        screenBitmapCopy = null
        cachedPixelsScreen = null
        cachedPixelsCondition = null
    }

    override fun setScreenBitmap(screenBitmap: Bitmap, metadata: String) = synchronized(lock) {
        if (isClosed) return

        screenBitmapCopy?.recycle()
        screenBitmapCopy = screenBitmap.copy(Bitmap.Config.ARGB_8888, true)
        screenDimensions.x = screenBitmapCopy?.width ?: 0
        screenDimensions.y = screenBitmapCopy?.height ?: 0
    }

    override fun detectCondition(
        conditionBitmap: Bitmap,
        conditionWidth: Int,
        conditionHeight: Int,
        detectionArea: Rect,
        threshold: Int,
    ): DetectionResult = synchronized(lock) {
        if (isClosed) return@synchronized detectionResult.copy()
        val screen = screenBitmapCopy ?: return@synchronized detectionResult.copy()

        val clampedRect = Rect(
            detectionArea.left.coerceAtLeast(0),
            detectionArea.top.coerceAtLeast(0),
            detectionArea.right.coerceAtMost(screen.width),
            detectionArea.bottom.coerceAtMost(screen.height),
        )
        if (clampedRect.width() <= 0 || clampedRect.height() <= 0) {
            detectionResult.setResults(false, 0, 0, 0.0)
            return detectionResult.copy()
        }

        val targetWidth = max(1, conditionWidth)
        val targetHeight = max(1, conditionHeight)
        val arraySize = targetWidth * targetHeight

        // Ensure buffers are large enough
        if (cachedPixelsScreen == null || cachedPixelsScreen!!.size < arraySize) {
            cachedPixelsScreen = IntArray(arraySize)
        }
        if (cachedPixelsCondition == null || cachedPixelsCondition!!.size < arraySize) {
            cachedPixelsCondition = IntArray(arraySize)
        }

        val pixelsScreen = cachedPixelsScreen!!
        val pixelsCondition = cachedPixelsCondition!!

        // 1. Prepare Screen Pixels
        val needScreenScaling = clampedRect.width() != targetWidth || clampedRect.height() != targetHeight
        if (needScreenScaling) {
             val screenRegion = Bitmap.createBitmap(
                screen,
                clampedRect.left,
                clampedRect.top,
                clampedRect.width(),
                clampedRect.height(),
            )
            val normalizedRegion = Bitmap.createScaledBitmap(screenRegion, targetWidth, targetHeight, true)
            normalizedRegion.getPixels(pixelsScreen, 0, targetWidth, 0, 0, targetWidth, targetHeight)
            
            if (normalizedRegion !== screenRegion) normalizedRegion.recycle()
            if (screenRegion !== screen) screenRegion.recycle()
        } else {
            // No scaling needed, read directly from screen bitmap
            screen.getPixels(
                pixelsScreen, 0, targetWidth,
                clampedRect.left, clampedRect.top,
                targetWidth, targetHeight
            )
        }

        // 2. Prepare Condition Pixels
        val needConditionScaling = conditionBitmap.width != targetWidth || conditionBitmap.height != targetHeight
        if (needConditionScaling) {
            val normalizedCondition = Bitmap.createScaledBitmap(conditionBitmap, targetWidth, targetHeight, true)
            normalizedCondition.getPixels(pixelsCondition, 0, targetWidth, 0, 0, targetWidth, targetHeight)
            if (normalizedCondition !== conditionBitmap) normalizedCondition.recycle()
        } else {
            conditionBitmap.getPixels(pixelsCondition, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        }

        // 3. Compute Difference
        val diff = computeDifference(pixelsScreen, pixelsCondition, arraySize)

        val diffPercent = (diff * 100.0).coerceIn(0.0, 100.0)
        val isMatch = diffPercent <= threshold
        val confidence = 1.0 - (diffPercent / 100.0)
        detectionResult.setResults(
            isDetected = isMatch,
            centerX = clampedRect.centerX(),
            centerY = clampedRect.centerY(),
            confidenceRate = confidence,
        )
        return@synchronized detectionResult.copy()
    }

    override fun releaseScreenBitmap(screenBitmap: Bitmap) = synchronized(lock) {
        if (isClosed) return
        screenBitmapCopy?.takeIf { it == screenBitmap }?.recycle()
        screenBitmapCopy = null
    }

    private fun computeDifference(pixelsScreen: IntArray, pixelsCondition: IntArray, size: Int): Double {
        var diffSum = 0L
        for (i in 0 until size) {
            val screenColor = pixelsScreen[i]
            val conditionColor = pixelsCondition[i]

            val sr = (screenColor shr 16) and 0xFF
            val sg = (screenColor shr 8) and 0xFF
            val sb = screenColor and 0xFF

            val cr = (conditionColor shr 16) and 0xFF
            val cg = (conditionColor shr 8) and 0xFF
            val cb = conditionColor and 0xFF

            diffSum += abs(sr - cr) + abs(sg - cg) + abs(sb - cb)
        }

        val maxDiff = size * 3 * 255.0
        return if (maxDiff == 0.0) 1.0 else diffSum / maxDiff
    }
}