
package com.buzbuz.taskengine.core.detection

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.Keep

/**
 * Native implementation of the image detector.
 * It uses OpenCv template matching algorithms to achieve condition detection on the screen.
 *
 * Debug flavour of the library is build against build artifacts of OpenCv in the debug folder.
 * Release flavour of the library is build against the sources of the OpenCv project, downloaded from github.
 */
class NativeDetector private constructor() : ImageDetector {

    companion object {
        fun newInstance(): NativeDetector? = try {
            System.loadLibrary("taskengine")
            NativeDetector()
        } catch (ex: UnsatisfiedLinkError) {
            null
        }
    }

    /** The results of the detection. Modified by native code. */
    private val detectionResult = DetectionResult()

    /** Native pointer of the detector object. */
    @Keep
    private var nativePtr: Long = newDetector()

    private var isClosed: Boolean = false

    override fun close() {
        if (isClosed) return

        isClosed = true
        deleteDetector()
    }

    override fun setScreenMetrics(metricsKey: String, screenBitmap: Bitmap, detectionQuality: Double) {
        if (isClosed) return

        if (detectionQuality < DETECTION_QUALITY_MIN || detectionQuality > DETECTION_QUALITY_MAX)
            throw IllegalArgumentException("Invalid detection quality")

        updateScreenMetrics(metricsKey, screenBitmap, detectionQuality)
    }

    override fun setupDetection(screenBitmap: Bitmap) {
        if (isClosed) return

        setScreenImage(screenBitmap)
    }

    override fun detectCondition(conditionBitmap: Bitmap, threshold: Int): DetectionResult {
        if (isClosed) return detectionResult.copy()

        detect(conditionBitmap, threshold, detectionResult)
        return detectionResult.copy()
    }

    override fun detectCondition(conditionBitmap: Bitmap, position: Rect, threshold: Int): DetectionResult {
        if (isClosed) return detectionResult.copy()

        detectAt(conditionBitmap, position.left, position.top, position.width(), position.height(), threshold, detectionResult)
        return detectionResult.copy()
    }

    /**
     * Creates the detector. Must be called before any other methods.
     * Call [close] to release resources once the detection process is finished.
     *
     * @return the pointer of the native detector object.
     */
    private external fun newDetector(): Long

    /**
     * Deletes the native detector.
     * Once called, this object can't be used anymore.
     */
    private external fun deleteDetector()

    /**
     * Native method for screen metrics setup.
     *
     * @param screenBitmap the content of the screen as a bitmap.
     * @param detectionQuality the quality of the detection. The higher the preciser, the lower the faster. Must be
     *                         contained in [DETECTION_QUALITY_MIN] and [DETECTION_QUALITY_MAX].
     */
    private external fun updateScreenMetrics(metricsKey: String, screenBitmap: Bitmap, detectionQuality: Double)

    /**
     * Native method for detection setup.
     *
     * @param screenBitmap the content of the screen as a bitmap.
     */
    private external fun setScreenImage(screenBitmap: Bitmap)

    /**
     * Native method for detecting if the bitmap is in the whole current screen bitmap.
     *
     * @param conditionBitmap the condition to detect in the screen.
     * @param threshold the allowed error threshold allowed for the condition.
     * @param result stores the results on this detection.
     */
    private external fun detect(conditionBitmap: Bitmap, threshold: Int, result: DetectionResult)

    /**
     * Native method for detecting if the bitmap is at a specific position in the current screen bitmap.
     *
     * @param conditionBitmap the condition to detect in the screen.
     * @param x the horizontal position of the condition.
     * @param y the vertical position of the condition.
     * @param width the width of the condition.
     * @param height the height of the condtion.
     * @param threshold the allowed error threshold allowed for the condition.
     * @param result stores the results on this detection.
     */
    private external fun detectAt(
        conditionBitmap: Bitmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
        result: DetectionResult
    )
}