package com.buzbuz.smartautoclicker.core.agent.action

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import com.buzbuz.smartautoclicker.core.agent.perception.UISnapshot
import com.buzbuz.smartautoclicker.core.domain.model.SmartActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes high-level [AgentAction]s using the system [SmartActionExecutor].
 */
class AgentActionExecutor(
    private val androidExecutor: SmartActionExecutor
) {

    /**
     * Executes the given [action].
     * @param action The action to perform.
     * @param snapshot The current UI snapshot, used to resolve element coordinates.
     * @return true if execution was successful (dispatched), false otherwise.
     */
    suspend fun execute(action: AgentAction, snapshot: UISnapshot): Boolean {
        // Clear previous state if needed
        androidExecutor.clearState()

        return try {
            when (action) {
                is AgentAction.Tap -> executeTap(action, snapshot)
                is AgentAction.LongPress -> executeLongPress(action, snapshot)
                is AgentAction.Type -> executeType(action)
                is AgentAction.Scroll -> executeScroll(action, snapshot)
                is AgentAction.Back -> withContext(Dispatchers.Main) { androidExecutor.executeGlobalBack() }
                is AgentAction.Home -> withContext(Dispatchers.Main) { androidExecutor.executeGlobalHome() }
                is AgentAction.Recents -> withContext(Dispatchers.Main) { androidExecutor.executeGlobalRecents() }
            }
        } catch (e: Exception) {
            Log.e("AgentExecutor", "Failed to execute action: $action", e)
            false
        }
    }

    private suspend fun executeTap(action: AgentAction.Tap, snapshot: UISnapshot): Boolean {
        val element = snapshot.elements[action.elementId] ?: return false
        val center = element.bounds.center()
        
        val path = Path().apply { moveTo(center.x.toFloat(), center.y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50)) // 50ms tap
            .build()

        return withContext(Dispatchers.Main) {
            androidExecutor.executeGesture(gesture)
            true
        }
    }

    private suspend fun executeLongPress(action: AgentAction.LongPress, snapshot: UISnapshot): Boolean {
        val element = snapshot.elements[action.elementId] ?: return false
        val center = element.bounds.center()

        val path = Path().apply { moveTo(center.x.toFloat(), center.y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 600)) // 600ms hold
            .build()

        return withContext(Dispatchers.Main) {
            androidExecutor.executeGesture(gesture)
            true
        }
    }

    private suspend fun executeType(action: AgentAction.Type): Boolean {
        return withContext(Dispatchers.Main) {
            androidExecutor.executeSetText(action.text)
        }
    }
    
    private suspend fun executeScroll(action: AgentAction.Scroll, snapshot: UISnapshot): Boolean {
        val bounds = Rect(0, 0, snapshot.screenWidth, snapshot.screenHeight)
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val centerX = w / 2
        val centerY = h / 2
        
        // Scroll distance: 80% of screen
        val scrollDistY = h * 0.8f
        val scrollDistX = w * 0.8f

        val start: android.graphics.PointF
        val end: android.graphics.PointF

        when (action.direction) {
            AgentAction.Scroll.Direction.UP -> {
                // Drag UP means content moves UP, finger moves DOWN? 
                // Wait. Action Scroll UP usually means "Show content above" -> Finger moves DOWN.
                // Or "Go DOWN the page" -> Finger moves UP.
                // Let's stick to standard convention: Scroll DOWN = Content moves UP (Reading usage).
                // So Finger moves UP from bottom to top.
                start = android.graphics.PointF(centerX, centerY + (scrollDistY / 2))
                end = android.graphics.PointF(centerX, centerY - (scrollDistY / 2))
            }
            AgentAction.Scroll.Direction.DOWN -> {
                // Finger moves DOWN (Top to Bottom)
                start = android.graphics.PointF(centerX, centerY - (scrollDistY / 2))
                end = android.graphics.PointF(centerX, centerY + (scrollDistY / 2))
            }
            AgentAction.Scroll.Direction.LEFT -> {
                // Swipe Left -> Finger Right to Left
                start = android.graphics.PointF(centerX + (scrollDistX / 2), centerY)
                end = android.graphics.PointF(centerX - (scrollDistX / 2), centerY)
            }
            AgentAction.Scroll.Direction.RIGHT -> {
                // Swipe Right -> Finger Left to Right
                start = android.graphics.PointF(centerX - (scrollDistX / 2), centerY)
                end = android.graphics.PointF(centerX + (scrollDistX / 2), centerY)
            }
        }
        
        val path = Path().apply {
            moveTo(start.x, start.y)
            lineTo(end.x, end.y)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
            
        return withContext(Dispatchers.Main) {
            androidExecutor.executeGesture(gesture)
            true
        }
    }
    
    private fun Rect.center(): android.graphics.Point {
        return android.graphics.Point(centerX(), centerY())
    }
}
