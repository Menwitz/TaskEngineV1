package com.buzbuz.smartautoclicker.core.agent.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.buzbuz.smartautoclicker.core.agent.TaskParser

/**
 * Receives remote commands (via ADB) to control the Agent.
 * Command: adb shell am broadcast -a com.buzbuz.smartautoclicker.agent.EXECUTE_TASK --es json_task '{"goal": "..."}'
 */
class RemoteCommandReceiver(
    private val onTaskReceived: (String) -> Unit,
    private val onToggleExploration: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_EXECUTE_TASK -> {
                val jsonTask = intent.getStringExtra(EXTRA_JSON_TASK)
                if (jsonTask != null) {
                    Log.i("RemoteCommand", "Received remote task: $jsonTask")
                    // Parse just to verify validity, but pass the goal/raw json to the listener
                    val goal = TaskParser().parseGoal(jsonTask)
                    AgentTelemetry.logTaskStart("remote_${System.currentTimeMillis()}", goal)
                    onTaskReceived(goal)
                } else {
                    Log.w("RemoteCommand", "Received EXECUTE_TASK but 'json_task' extra was missing.")
                }
            }
            ACTION_TOGGLE_EXPLORATION -> {
                Log.i("RemoteCommand", "Received TOGGLE_EXPLORATION")
                onToggleExploration()
            }
        }
    }

    companion object {
        const val ACTION_EXECUTE_TASK = "com.buzbuz.smartautoclicker.agent.EXECUTE_TASK"
        const val ACTION_TOGGLE_EXPLORATION = "com.buzbuz.smartautoclicker.agent.TOGGLE_EXPLORATION"
        const val EXTRA_JSON_TASK = "json_task"
    }
}
