
package com.buzbuz.smartautoclicker.localservice

import android.content.Intent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

import kotlinx.coroutines.flow.Flow

interface ILocalService {
    /** State of the Autonomous Agent (true if running). */
    val agentState: Flow<Boolean>

    fun startSmartScenario(resultCode: Int, data: Intent, scenario: Scenario)
    fun stop()
    
    /** Start the Autonomous Agent with a specific goal. */
    fun startAgent(goal: String)
    /** Stop the Autonomous Agent. */
    fun stopAgent()
    
    fun release()
}