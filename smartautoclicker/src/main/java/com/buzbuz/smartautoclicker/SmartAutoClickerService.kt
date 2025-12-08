package com.buzbuz.smartautoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.Notification
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat

import com.buzbuz.smartautoclicker.actions.ServiceActionExecutor
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.base.extensions.requestFilterKeyEvents
import com.buzbuz.smartautoclicker.core.base.extensions.startForegroundMediaProjectionServiceCompat
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.common.quality.domain.QualityMetricsMonitor
import com.buzbuz.smartautoclicker.core.common.quality.domain.QualityRepository
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.SmartActionExecutor
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.domain.model.NotificationRequest
import com.buzbuz.smartautoclicker.localservice.LocalService
import com.buzbuz.smartautoclicker.localservice.LocalServiceProvider
import android.view.Display
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileDescriptor
import java.io.PrintWriter
import javax.inject.Inject

import android.graphics.*
import android.os.*
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import java.io.FileOutputStream


import com.buzbuz.smartautoclicker.core.agent.perception.AccessibilityParser
import com.buzbuz.smartautoclicker.core.agent.remote.RemoteCommandReceiver
import android.content.IntentFilter
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.agent.AgentLoop
import com.buzbuz.smartautoclicker.core.agent.action.AgentActionExecutor
import com.buzbuz.smartautoclicker.core.agent.brain.MockLLMClient
import com.buzbuz.smartautoclicker.core.agent.brain.OpenAIClient
import com.buzbuz.smartautoclicker.core.agent.exploration.ExplorationService

/**
 * AccessibilityService implementation for the SmartAutoClicker.
 *
 * Started automatically by Android once the user has defined this service has an accessibility service, it provides
 * an API to start and stop the DetectorEngine correctly in order to display the overlay UI and record the screen for
 * clicks detection.
 * This API is offered through the [LocalService] class, which is instantiated in the [LocalServiceProvider] object.
 * This system is used instead of the usual binder interface because an [AccessibilityService] already has its own
 * binder and it can't be changed. To access this local service, use [LocalServiceProvider].
 *
 * We need this service to be an accessibility service in order to inject the detected event on the currently
 * displayed activity. This injection is made by the [dispatchGesture] method, which is called everytime an event has
 * been detected.
 */

@AndroidEntryPoint
class SmartAutoClickerService : AccessibilityService(), SmartActionExecutor {

    private val localServiceProvider = LocalServiceProvider

    private val localService: LocalService?
        get() = localServiceProvider.localServiceInstance as? LocalService

    @Inject lateinit var overlayManager: OverlayManager
    @Inject lateinit var displayConfigManager: DisplayConfigManager
    @Inject lateinit var detectionRepository: DetectionRepository
    @Inject lateinit var bitmapManager: BitmapRepository
    @Inject lateinit var qualityRepository: QualityRepository
    @Inject lateinit var qualityMetricsMonitor: QualityMetricsMonitor
    @Inject lateinit var appComponentsProvider: AppComponentsProvider

    private var serviceActionExecutor: ServiceActionExecutor? = null
    private val accessibilityParser = AccessibilityParser()
    
    // Agent Components
    private var remoteCommandReceiver: RemoteCommandReceiver? = null
    private var agentLoop: AgentLoop? = null
    private var explorationService: ExplorationService? = null
    private var explorationJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()

        qualityMetricsMonitor.onServiceConnected()
        serviceActionExecutor = ServiceActionExecutor(this)

        // Initialize Agent Components
        setupAgent()


        localServiceProvider.setLocalService(
            LocalService(
                context = this,
                overlayManager = overlayManager,
                appComponentsProvider = appComponentsProvider,
                detectionRepository = detectionRepository,
                androidExecutor = this,
                onStart = ::onLocalServiceStarted,
                onStop = ::onLocalServiceStopped,
                notificationId = FOREGROUND_NOTIFICATION_ID,
            )
        )
    }

    override fun onUnbind(intent: Intent?): Boolean {
        localServiceProvider.localServiceInstance?.apply {
            stop()
            release()
        }
        localServiceProvider.setLocalService(null)

        // Cleanup Agent
        if (remoteCommandReceiver != null) {
            unregisterReceiver(remoteCommandReceiver)
            remoteCommandReceiver = null
        }
        agentLoop = null

        qualityMetricsMonitor.onServiceUnbind()
        serviceActionExecutor = null
        return super.onUnbind(intent)
    }

    private fun setupAgent() {
        // Wire up the Remote Command Receiver
        remoteCommandReceiver = RemoteCommandReceiver(
            onTaskReceived = { goal -> startAgentTask(goal) },
            onToggleExploration = { toggleExploration() }
        )
        val filter = IntentFilter().apply {
            addAction(RemoteCommandReceiver.ACTION_EXECUTE_TASK)
            addAction(RemoteCommandReceiver.ACTION_TOGGLE_EXPLORATION)
        }
        registerReceiver(remoteCommandReceiver, filter, RECEIVER_EXPORTED) // Needs export for ADB

        // Prepare the Loop (Brain + Hands + Eyes)
        val apiKey = BuildConfig.OPENAI_API_KEY
        val brain = if (apiKey.isNotBlank()) {
            OpenAIClient(apiKey)
        } else {
            Log.w(TAG, "No OPENAI_API_KEY found in BuildConfig. Using Mock Brain.")
            MockLLMClient()
        }

        val actionExecutor = AgentActionExecutor(this) // 'this' implements SmartActionExecutor
        // Note: accessibilityParser is already instantiated
        
        // Exploration Service
        val navigation = com.buzbuz.smartautoclicker.core.agent.memory.NavigationGraph()
        explorationService = ExplorationService(accessibilityParser, actionExecutor, navigation)

        agentLoop = AgentLoop(
            parser = accessibilityParser,
            brain = brain,
            actionExecutor = actionExecutor,
            context = this
        )
    }

    private fun startAgentTask(goal: String) {
        Log.i(TAG, "Starting Agent Task: $goal")
        serviceScope.launch {
            val dm = resources.displayMetrics
            agentLoop?.runTask(
                goal = goal,
                rootProvider = { rootInActiveWindow },
                screenMetrics = dm.widthPixels to dm.heightPixels
            )
        }
    }

    private fun onLocalServiceStarted(scenarioId: Long, isSmart: Boolean, serviceNotification: Notification?) {
        qualityMetricsMonitor.onServiceForegroundStart()
        serviceActionExecutor?.reset()

        serviceNotification?.let {
            startForegroundMediaProjectionServiceCompat(FOREGROUND_NOTIFICATION_ID, it)
        }
        requestFilterKeyEvents(true)

        displayConfigManager.startMonitoring(this)
    }

    private fun onLocalServiceStopped() {
        qualityMetricsMonitor.onServiceForegroundEnd()

        requestFilterKeyEvents(false)
        stopForeground(STOP_FOREGROUND_REMOVE)

        displayConfigManager.stopMonitoring()
        bitmapManager.clearCache()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean =
        localService?.onKeyEvent(event) ?: super.onKeyEvent(event)

    override suspend fun executeGesture(gestureDescription: GestureDescription) {
        serviceActionExecutor?.safeDispatchGesture(gestureDescription)
    }

    override fun executeStartActivity(intent: Intent) {
        serviceActionExecutor?.safeStartActivity(intent)
    }

    override fun executeSendBroadcast(intent: Intent) {
        serviceActionExecutor?.safeSendBroadcast(intent)
    }

    override fun executeNotification(notification: NotificationRequest) {
        val channelId = "scenario_notification_channel"
        val manager = getSystemService(NotificationManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_smart_auto_clicker)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        manager.notify(notification.actionId.toInt(), builder.build())
    }

    override fun clearState() {
        // Nothing to clear now that notifications are disabled.
    }

    /** Return screen bounds from the active root node; fallback to display metrics. */
    override fun getScreenBounds(): Rect? {
        val root = rootInActiveWindow
        if (root != null) {
            val out = Rect()
            root.getBoundsInScreen(out)
            if (out.width() > 0 && out.height() > 0) return out
        }
        val dm = resources.displayMetrics
        return Rect(0, 0, dm.widthPixels, dm.heightPixels)
    }

    override fun executeGlobalBack(): Boolean =
        performGlobalAction(GLOBAL_ACTION_BACK)

    override fun executeGlobalHome(): Boolean =
        performGlobalAction(GLOBAL_ACTION_HOME)

    override fun executeGlobalRecents(): Boolean =
        performGlobalAction(GLOBAL_ACTION_RECENTS)

    override fun executeGlobalNotifications(): Boolean =
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    override fun executeGlobalQuickSettings(): Boolean =
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    override fun executeScreenshot(roi: Rect?, savePath: String?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false

        var ok = false
        val latch = CountDownLatch(1)

        // API 30 signature: takeScreenshot(displayId, executor, TakeScreenshotCallback)
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    try {
                        val hb = result.hardwareBuffer     // API 30
                        val cs = result.colorSpace         // API 30
                        if (hb != null && cs != null) {
                            val src = Bitmap.wrapHardwareBuffer(hb, cs)
                            if (src != null) {
                                val bmp = if (roi != null) {
                                    val safe = clipRoiTo(src.width, src.height, roi)
                                    Bitmap.createBitmap(
                                        src, safe.left, safe.top, safe.width(), safe.height()
                                    )
                                } else {
                                    src.copy(Bitmap.Config.ARGB_8888, false)
                                }
                                if (savePath != null) savePng(bmp, savePath)
                                ok = true
                            }
                            // HardwareBuffer must be closed
                            hb.close()
                        }
                    } catch (_: Throwable) {
                        // keep ok = false
                    } finally {
                        // ScreenshotResult doesn't need explicit close() call here
                        latch.countDown()
                    }
                }

                override fun onFailure(errorCode: Int) {
                    latch.countDown()
                }
            }
        )

        // Wait briefly so callers can get a synchronous boolean
        latch.await(750, TimeUnit.MILLISECONDS)
        return ok
    }

    override fun executeSetText(text: String): Boolean {
        val target = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /** Best-effort IME key sequence. ENTER is supported; others are no-ops today. */
    override fun executeImeKeySequence(codes: List<Int>, intervalMs: Long): Boolean {
        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false

        fun setText(newText: String): Boolean {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        var any = false
        var current = node.text?.toString() ?: ""

        for (code in codes) {
            val changed = when (code) {
                KeyEvent.KEYCODE_ENTER -> {
                    current += "\n"; true
                }
                KeyEvent.KEYCODE_DEL -> {
                    if (current.isNotEmpty()) { current = current.dropLast(1); true } else false
                }
                else -> false // expand later if needed
            }
            if (changed) {
                any = setText(current) || any
                if (intervalMs > 0) SystemClock.sleep(intervalMs)
            }
        }
        return any
    }

    /** Short human-like tap in a safe area to dismiss keyboard/dialogs. */
    override fun tapSafeArea(): Boolean {
        val path = Path().apply {
            val x = 40f
            val y = 80f
            moveTo(x, y)
            lineTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 60)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    // ---------- utils ----------

    private fun clipRoiTo(w: Int, h: Int, r: Rect): Rect =
        Rect(
            r.left.coerceIn(0, w),
            r.top.coerceIn(0, h),
            r.right.coerceIn(0, w),
            r.bottom.coerceIn(0, h),
        ).also {
            if (it.right <= it.left) it.right = (it.left + 1).coerceAtMost(w)
            if (it.bottom <= it.top) it.bottom = (it.top + 1).coerceAtMost(h)
        }

    private fun savePng(bmp: Bitmap, path: String) {
        val file = if (path.startsWith("/")) File(path) else File(filesDir, path)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
    }

    /**
     * Dump the state of the service via adb.
     * adb shell "dumpsys activity service com.buzbuz.smartautoclicker"
     */
    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        if (writer == null) return

        writer.append("* SmartAutoClickerService:").println()
        writer.append(Dumpable.DUMP_DISPLAY_TAB)
            .append("- isStarted=").append("${localService?.isStarted ?: false}; ")
            .println()

        displayConfigManager.dump(writer)
        bitmapManager.dump(writer)
        overlayManager.dump(writer)
        detectionRepository.dump(writer)
        serviceActionExecutor?.dump(writer)
        qualityRepository.dump(writer)

        writer.append("* Agent Perception:").println()
        val root = rootInActiveWindow
        if (root != null) {
            try {
                val dm = resources.displayMetrics
                val snapshot = accessibilityParser.parse(root, dm.widthPixels, dm.heightPixels)
                writer.append(Dumpable.DUMP_DISPLAY_TAB).append("Snapshot Elements: ${snapshot.elements.size}").println()
                snapshot.elements.values.take(20).forEach { el ->
                   writer.append(Dumpable.DUMP_DISPLAY_TAB).append("  [${el.id}] ${el.viewIdResourceName ?: ""} '${el.text ?: ""}' ${el.contentDescription ?: ""} ${el.bounds}").println()
                }
            } catch (e: Exception) {
                writer.append(Dumpable.DUMP_DISPLAY_TAB).append("Error parsing UI: ${e.message}").println()
            }
        } else {
            writer.append(Dumpable.DUMP_DISPLAY_TAB).append("Root is null").println()
        }
    }

    override fun onInterrupt() { /* Unused */ }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Unused */ }
    private fun toggleExploration() {
        if (explorationJob?.isActive == true) {
            Log.i(TAG, "Stopping Exploration...")
            explorationJob?.cancel()
            explorationJob = null
            // Optional: Notify user
        } else {
            Log.i(TAG, "Starting Exploration...")
            explorationJob = serviceScope.launch {
                val dm = resources.displayMetrics
                val width = dm.widthPixels
                val height = dm.heightPixels
                
                explorationService?.startExploration(
                    rootProvider = { rootInActiveWindow },
                    screenMetrics = Pair(width, height)
                )
            }
        }
    }
}

/** Tag for the logs. */
private const val TAG = "SmartAutoClickerService"
private const val FOREGROUND_NOTIFICATION_ID = 1001