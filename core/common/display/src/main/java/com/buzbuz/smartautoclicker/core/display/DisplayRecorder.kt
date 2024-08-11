
package com.buzbuz.smartautoclicker.core.display

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Record the screen and provide [Image] from it.
 *
 * Uses the [MediaProjection] API to create a [VirtualDisplay] not shown to the user and containing a copy of the
 * user device screen content. An [ImageReader] is attached to this display in order to monitor every new frame
 * displayed on the screen, received in the form of an [Image]. Then, process those Image with ScenarioProcessor
 * according to the current mode (capture/detection). All Image processing code is executed on a background thread
 * (methods annotated with [WorkerThread]), and all results callbacks are executed on the main thread (the thread that
 * has instantiated this class).
 *
 * To start recording, call [startProjection] (see method documentation for permission management). This must be done
 * before any other action on this object. Once the recording isn't necessary anymore, you must stop it by calling
 * [stopProjection] in order to release all resources associated with this object.
 */
@MainThread
@Singleton
class DisplayRecorder @Inject internal constructor() {

    /** Synchronization mutex. */
    private val mutex = Mutex()
    /**
     * The token granting applications the ability to capture screen contents by creating a [VirtualDisplay].
     * Can only be not null if the user have granted the permission displayed by
     * [MediaProjectionManager.createScreenCaptureIntent].
     */
    private var projection: MediaProjection? = null
    /** Virtual display capturing the content of the screen. */
    private var virtualDisplay: VirtualDisplay? = null
    /** Listener to notify upon projection ends. */
    private var stopListener: (() -> Unit)? = null
    /** Allow access to [Image] rendered into the surface view of the [VirtualDisplay] */
    private var imageReader: ImageReader? = null

    /** Cache for the current frame. Interpreted from an [Image]. */
    private var latestAcquiredFrameBitmap: Bitmap? = null

    /**
     * Start the media projection.
     *
     * Initialize all values required for screen recording and start the thread managing the processing. This method
     * should be called before anything else in this class. Once you are done with the screen recording, you should
     * call [stopProjection] in order to release all resources.
     *
     * Recording the screen requires the media projection permission code and its data intent, they both can be
     * retrieved using the results of the activity intent provided by [MediaProjectionManager.createScreenCaptureIntent]
     * (this Intent shows the dialog warning about screen recording privacy). Any attempt to call this method without
     * the correct screen capture intent result will leads to a crash.
     *
     * If the screen record was already started, this method will have no effect.
     *
     * @param context the Android context.
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param stoppedListener listener called when the projection have been stopped unexpectedly.
     */
    suspend fun startProjection(context: Context, resultCode: Int, data: Intent, stoppedListener: () -> Unit) = mutex.withLock {
        if (projection != null) {
            Log.w(TAG, "Attempting to start media projection while already started.")
            return
        }

        Log.d(TAG, "Start media projection")

        stopListener = stoppedListener
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        projection = projectionManager.getMediaProjection(resultCode, data).apply {
            registerCallback(projectionCallback, Handler(Looper.getMainLooper()))
        }
    }

    /**
     * Start the screen record.
     * This method should not be called from the main thread, but the processing thread.
     *
     * @param context the Android context.
     * @param displaySize the size of the display, in pixels.
     */
    suspend fun startScreenRecord(context: Context, displaySize: Point): Unit = mutex.withLock {
        if (projection == null || imageReader != null) {
            Log.w(TAG, "Attempting to start screen record while already started.")
            return
        }

        Log.d(TAG, "Start screen record with display size $displaySize")

        @SuppressLint("WrongConstant")
        imageReader = ImageReader.newInstance(displaySize.x, displaySize.y, PixelFormat.RGBA_8888, 2)
        try {
            virtualDisplay = projection!!.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME, displaySize.x, displaySize.y, context.resources.configuration.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader!!.surface, null,
                null)
        } catch (sEx: SecurityException) {
            Log.w(TAG, "Screencast permission is no longer valid, stopping Smart AutoClicker...", sEx)
            stopListener?.invoke()
        }
    }

    suspend fun resizeDisplay(context: Context, displaySize: Point): Unit = mutex.withLock {
        if (virtualDisplay == null || imageReader == null) return

        Log.d(TAG, "Resizing virtual display to $displaySize")

        virtualDisplay?.let { vDisplay ->
            imageReader?.close()
            imageReader = ImageReader.newInstance(displaySize.x, displaySize.y, PixelFormat.RGBA_8888, 2)

            vDisplay.surface = imageReader?.surface
            vDisplay.resize(
                displaySize.x,
                displaySize.y,
                context.resources.configuration.densityDpi,
            )
        }
    }

    /** @return the last image of the screen, or null if they have been processed. */
    suspend fun acquireLatestBitmap(): Bitmap? = mutex.withLock {
        imageReader?.acquireLatestImage()?.use { image ->
            latestAcquiredFrameBitmap = image.toBitmap(latestAcquiredFrameBitmap)
            latestAcquiredFrameBitmap
        }
    }

    suspend fun takeScreenshot(completion: suspend (Bitmap) -> Unit) {
        var finished = false
        do {
            imageReader?.acquireLatestImage()?.use { image ->
                completion(
                    Bitmap.createBitmap(
                        image.toBitmap(latestAcquiredFrameBitmap),
                        0,
                        0,
                        image.width,
                        image.height,
                    )
                )
                finished = true
            }

        } while (!finished)
    }

    /**
     * Stop the screen recording.
     * This method should not be called from the main thread, but the processing thread.
     */
    suspend fun stopScreenRecord() = mutex.withLock {
        Log.d(TAG, "Stop screen record")

        virtualDisplay?.apply {
            release()
            virtualDisplay = null
        }
        imageReader?.apply {
            close()
            imageReader = null
        }
    }

    /**
     * Stop the media projection previously started with [startProjection].
     *
     * This method will free/close any resources related to screen recording. If a detection was started, it will be
     * stopped. If the screen record wasn't started, this method will have no effect.
     */
    suspend fun stopProjection() {
        Log.d(TAG, "Stop media projection")

        stopScreenRecord()

        mutex.withLock {
            projection?.apply {
                unregisterCallback(projectionCallback)
                stop()
            }
            projection = null
            stopListener = null
        }
    }

    /** Called when the user have stopped the projection by clicking on the 'Cast' icon in the status bar. */
    private val projectionCallback = object : MediaProjection.Callback() {

        override fun onStop() {
            Log.i(TAG, "Projection stopped by the user")
            // We only notify, we let the detector take care of calling stopScreenRecord
            stopListener?.invoke()
        }
    }
}

/**
 * Transform an Image into a bitmap.
 *
 * @param resultBitmap a bitmap to use as a cache in order to avoid instantiating an new one. If null, a new one is
 *                     created.
 * @return the bitmap corresponding to the image. If [resultBitmap] was provided, it will be the same object.
 */
private fun Image.toBitmap(resultBitmap: Bitmap? = null): Bitmap {
    var bitmap = resultBitmap
    val imageWidth = width + (planes[0].rowStride - planes[0].pixelStride * width) / planes[0].pixelStride

    if (bitmap == null) {
        bitmap = Bitmap.createBitmap(imageWidth, height, Bitmap.Config.ARGB_8888)
    } else if (bitmap.width != imageWidth || bitmap.height != height) {
        try {
            bitmap.reconfigure(imageWidth, height, Bitmap.Config.ARGB_8888)
        } catch (ex: IllegalArgumentException) {
            bitmap = Bitmap.createBitmap(imageWidth, height, Bitmap.Config.ARGB_8888)
        }
    }

    bitmap.copyPixelsFromBuffer(planes[0].buffer)
    return bitmap
}

/** Tag for logs. */
private const val TAG = "DisplayRecorder"
/** Name of the virtual display generating [Image]. */
internal const val VIRTUAL_DISPLAY_NAME = "SmartAutoClicker"