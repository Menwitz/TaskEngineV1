package com.buzbuz.smartautoclicker.core.agent.action

/**
 * High-level semantic actions that the Agent can perform.
 */
sealed class AgentAction {
    
    /** Tap on a specific element identified by [elementId]. */
    data class Tap(val elementId: Int) : AgentAction()
    
    /** Long press on a specific element. */
    data class LongPress(val elementId: Int) : AgentAction()
    
    /** Input text into the currently focused field. */
    data class Type(val text: String) : AgentAction()
    
    /** Scroll in a direction. */
    data class Scroll(val direction: Direction) : AgentAction() {
        enum class Direction { UP, DOWN, LEFT, RIGHT }
    }
    
    /** Navigate Back. */
    object Back : AgentAction()
    
    /** Navigate Home. */
    object Home : AgentAction()
    
    /** Open Recents. */
    object Recents : AgentAction()

    override fun toString(): String = when(this) {
        is Tap -> "Tap($elementId)"
        is LongPress -> "LongPress($elementId)"
        is Type -> "Type('$text')"
        is Scroll -> "Scroll($direction)"
        Back -> "Back"
        Home -> "Home"
        Recents -> "Recents"
    }
}
