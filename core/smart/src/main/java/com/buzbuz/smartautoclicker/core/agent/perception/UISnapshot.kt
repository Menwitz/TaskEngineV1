package com.buzbuz.smartautoclicker.core.agent.perception

import android.graphics.Rect

/**
 * Represents a snapshot of the UI structure, simplified for the Agent.
 */
data class UISnapshot(
    val rootId: Int,
    val elements: Map<Int, UIElement>,
    val screenWidth: Int,
    val screenHeight: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * A semantic element in the UI.
 */
data class UIElement(
    val id: Int,
    val text: String?,
    val contentDescription: String?,
    val viewIdResourceName: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val childrenIds: List<Int>,
    val parentId: Int?
) {
    fun getLabel(): String? = text ?: contentDescription
}
