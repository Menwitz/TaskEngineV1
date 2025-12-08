package com.buzbuz.smartautoclicker.core.agent.remote

import android.util.Log
import org.json.JSONObject

/**
 * Structured logger for Agent Telemetry.
 * Outputs JSON lines to Logcat for easy scraping by fleet management tools.
 */
object AgentTelemetry {
    
    private const val TAG = "AGENT_TELEMETRY"

    fun logTaskStart(taskId: String, goal: String) {
        logEvent("TASK_START", mapOf("taskId" to taskId, "goal" to goal))
    }

    fun logStep(step: Int, action: String, screenId: Int) {
        logEvent("STEP", mapOf("step" to step, "action" to action, "screenId" to screenId))
    }

    fun logTaskEnd(taskId: String, status: String, reason: String? = null) {
        logEvent("TASK_END", mapOf("taskId" to taskId, "status" to status, "reason" to reason))
    }

    private fun logEvent(type: String, data: Map<String, Any?>) {
        val json = JSONObject()
        json.put("event", type)
        json.put("timestamp", System.currentTimeMillis())
        data.forEach { (k, v) -> json.put(k, v) }
        
        Log.i(TAG, json.toString())
    }
}
