package com.buzbuz.smartautoclicker.core.agent.memory

import com.buzbuz.smartautoclicker.core.agent.action.AgentAction
import com.buzbuz.smartautoclicker.core.agent.perception.UISnapshot

/**
 * A graph representing the map of an application.
 */
class NavigationGraph {
    private val nodes = mutableMapOf<Int, ScreenNode>()
    private val edges = mutableListOf<Transition>()

    fun addOrGetNode(snapshot: UISnapshot): ScreenNode {
        val signature = calculateSignature(snapshot)
        return nodes.getOrPut(signature) {
            ScreenNode(signature, snapshot)
        }
    }

    fun addTransition(from: ScreenNode, to: ScreenNode, action: AgentAction) {
        val transition = Transition(from.signature, to.signature, action)
        if (!edges.contains(transition)) {
            edges.add(transition)
        }
    }
    
    fun getTransitionsFrom(node: ScreenNode): List<Transition> {
        return edges.filter { it.fromSignature == node.signature }
    }

    /**
     * Calculates a stable hash for the screen structure.
     * Use resource IDs and structure, ignore volatile text/bounds if needed.
     */
    private fun calculateSignature(snapshot: UISnapshot): Int {
        // Simple signature strategy: Hash of all resource IDs + Child Count
        // In production, we'd want a more robust "Structural Hash"
        var hash = 17
        snapshot.elements.values.forEach { 
             hash = hash * 31 + (it.viewIdResourceName?.hashCode() ?: 0)
             hash = hash * 31 + it.childrenIds.size
        }
        return hash
    }
}

data class ScreenNode(
    val signature: Int,
    val snapshot: UISnapshot, // Keep one example snapshot
    val discoveredAt: Long = System.currentTimeMillis()
)

data class Transition(
    val fromSignature: Int,
    val toSignature: Int,
    val action: AgentAction
)
