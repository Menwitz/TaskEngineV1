package com.buzbuz.smartautoclicker.core.agent

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.buzbuz.smartautoclicker.core.agent.action.AgentAction
import com.buzbuz.smartautoclicker.core.agent.action.AgentActionExecutor
import com.buzbuz.smartautoclicker.core.agent.brain.LLMClient
import com.buzbuz.smartautoclicker.core.agent.brain.Message
import com.buzbuz.smartautoclicker.core.agent.brain.Role
import com.buzbuz.smartautoclicker.core.agent.perception.AccessibilityParser
import com.buzbuz.smartautoclicker.core.agent.perception.UISnapshot
import kotlinx.coroutines.delay
import org.json.JSONObject
import com.buzbuz.smartautoclicker.core.agent.action.AgentAction.Tap
import com.buzbuz.smartautoclicker.core.agent.action.AgentAction.Type
import com.buzbuz.smartautoclicker.core.agent.action.AgentAction.Scroll
import com.buzbuz.smartautoclicker.core.agent.action.AgentAction.Back
import com.buzbuz.smartautoclicker.core.agent.action.AgentAction.Home


/**
 * The autonomous agent loop.
 * Observe -> Orient -> Decide -> Act.
 */
class AgentLoop(
    private val parser: AccessibilityParser,
    private val brain: LLMClient,
    private val actionExecutor: AgentActionExecutor,
    private val context: Context
) {

    private val history = mutableListOf<String>()
    
    suspend fun runTask(
        goal: String, 
        rootProvider: () -> AccessibilityNodeInfo?,
        screenMetrics: Pair<Int, Int>
    ) {
        history.clear()
        Log.i("AgentLoop", "Starting task: $goal")
        
        var steps = 0
        val maxSteps = 20 // Safety limit
        
        while (steps < maxSteps) {
            steps++
            
            // 1. Observe
            val root = rootProvider()
            if (root == null) {
                Log.w("AgentLoop", "Root node is null, retrying...")
                delay(1000)
                continue
            }
            
            val snapshot = parser.parse(root, screenMetrics.first, screenMetrics.second)
            
            // 2. Decide
            val action = decideNextAction(goal, snapshot)
            
            if (action == null) {
                Log.i("AgentLoop", "Agent decided to stop or failed to decide.")
                break
            }
            
            Log.i("AgentLoop", "Executing: $action")
            
            // 3. Act
            val success = actionExecutor.execute(action, snapshot)
            
            // 4. Update History
            if (success) {
                history.add("Executed: $action")
            } else {
                history.add("Failed to execute: $action")
            }
            
            // Wait for UI to settle
            delay(2000)
        }
    }
    
    private suspend fun decideNextAction(goal: String, snapshot: UISnapshot): AgentAction? {
        val prompt = buildPrompt(goal, snapshot)
        
        val messages = listOf(
            Message(Role.SYSTEM, SYSTEM_PROMPT),
            Message(Role.USER, prompt)
        )
        
        val response = brain.generateResponse(messages)
        
        try {
            val jsonString = response.content.trim().let {
                if (it.startsWith("```json")) it.removePrefix("```json").removeSuffix("```").trim()
                else it
            }
            
            val json = JSONObject(jsonString)
            return when (val actionType = json.getString("action")) {
                "tap" -> Tap(json.getInt("id"))
                "type" -> Type(json.getString("text"))
                "scroll" -> {
                    val direction = when(json.getString("direction").lowercase()) {
                        "up" -> Scroll.Direction.UP
                        "down" -> Scroll.Direction.DOWN
                        "left" -> Scroll.Direction.LEFT
                        "right" -> Scroll.Direction.RIGHT
                        else -> return null
                    }
                    Scroll(direction)
                }
                "back" -> Back
                "home" -> Home
                "stop" -> null
                else -> {
                    Log.w("AgentLoop", "Unknown action: $actionType")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("AgentLoop", "Failed to parse LLM response: ${response.content}", e)
            return null
        }
    }

    private fun buildPrompt(goal: String, snapshot: UISnapshot): String {
        val elementsDesc = snapshot.elements.values.joinToString("\n") { 
            "${it.id} | ${it.getLabel() ?: "No text"} | ${it.viewIdResourceName ?: ""} | ${it.bounds}" 
        }
        
        return """
            GOAL: $goal
            
            HISTORY:
            ${history.joinToString("\n")}
            
            CURRENT SCREEN (Visible Elements):
            ID | Text/Desc | ResourceID | Bounds
            $elementsDesc
            
            Instructions:
            - Analyze the screen and history.
            - Output a JSON object with the next action.
            - Example: { "action": "tap", "id": 12 }
        """.trimIndent()
    }
    
    companion object {
        private const val SYSTEM_PROMPT = """
            You are an autonomous Android agent. 
            Your job is to achieve the user's goal by interacting with the UI.
            You can Tap, Type, Scroll, or press Back/Home.
            Always rely on the numeric IDs provided in the screen description.
        """
    }
}
