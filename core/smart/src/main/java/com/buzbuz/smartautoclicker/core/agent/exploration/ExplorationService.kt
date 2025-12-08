package com.buzbuz.smartautoclicker.core.agent.exploration

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.buzbuz.smartautoclicker.core.agent.action.AgentAction
import com.buzbuz.smartautoclicker.core.agent.action.AgentActionExecutor
import com.buzbuz.smartautoclicker.core.agent.memory.NavigationGraph
import com.buzbuz.smartautoclicker.core.agent.memory.ScreenNode
import com.buzbuz.smartautoclicker.core.agent.perception.AccessibilityParser
import kotlinx.coroutines.delay

/**
 * Service to autonomously explore an application.
 */
class ExplorationService(
    private val parser: AccessibilityParser,
    private val actionExecutor: AgentActionExecutor,
    private val navigationGraph: NavigationGraph
) {

    private var isExploring = false
    
    suspend fun startExploration(
        rootProvider: () -> AccessibilityNodeInfo?,
        screenMetrics: Pair<Int, Int>
    ) {
        isExploring = true
        Log.i("Exploration", "Starting autonomous exploration...")
        
        while (isExploring) {
            val root = rootProvider()
            if (root == null) { delay(1000); continue }
            
            // 1. Parse Current State
            val snapshot = parser.parse(root, screenMetrics.first, screenMetrics.second)
            val currentNode = navigationGraph.addOrGetNode(snapshot)
            
            // 2. Decide next exploration step (Simple DFS/Greedy)
            val action = selectNextExplorationAction(currentNode)
            
            if (action == null) {
                Log.i("Exploration", "No more actions to explore in this branch. Backtracking...")
                // In a real implementation: Execute BACK
                // For safety in this POC: Just stopping.
                isExploring = false
                break
            }
            
            // 3. Execute
            Log.i("Exploration", "Exploring action: $action")
            val success = actionExecutor.execute(action, snapshot)
            delay(2000) // Wait for settle
            
            if (success) {
                // 4. Observe Result
                val newRoot = rootProvider() ?: return
                val newSnapshot = parser.parse(newRoot, screenMetrics.first, screenMetrics.second)
                val newNode = navigationGraph.addOrGetNode(newSnapshot)
                
                // 5. Record Edge
                navigationGraph.addTransition(currentNode, newNode, action)
            }
        }
    }
    
    private fun selectNextExplorationAction(node: ScreenNode): AgentAction? {
        val existingTransitions = navigationGraph.getTransitionsFrom(node)
        val triedActionIds = existingTransitions
            .mapNotNull { (it.action as? AgentAction.Tap)?.elementId }
            .toSet()
            
        // Find a clickable element that hasn't been tapped yet
        val candidate = node.snapshot.elements.values.firstOrNull { 
            it.isClickable && !triedActionIds.contains(it.id)
        }
        
        return candidate?.let { AgentAction.Tap(it.id) }
    }
    
    fun stop() {
        isExploring = false
    }
}
