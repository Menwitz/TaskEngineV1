package com.buzbuz.smartautoclicker.core.agent

import org.json.JSONObject

/**
 * Parses a JSON task definition and returns the goal string.
 * Example JSON:
 * {
 *   "task_id": "setup_wifi",
 *   "goal": "Connect to WiFi named 'Guest'",
 *   "variables": { ... }
 * }
 */
class TaskParser {

    fun parseGoal(jsonString: String): String {
        return try {
            val json = JSONObject(jsonString)
            if (json.has("goal")) {
                json.getString("goal")
            } else {
                // Fallback: use the raw string as the goal if not valid JSON or missing "goal"
                jsonString
            }
        } catch (e: Exception) {
            // Not a JSON object, treat entire string as the goal
            jsonString
        }
    }
}
