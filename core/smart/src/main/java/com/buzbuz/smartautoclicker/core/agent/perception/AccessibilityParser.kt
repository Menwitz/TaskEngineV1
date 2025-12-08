package com.buzbuz.smartautoclicker.core.agent.perception

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Parses AccessibilityNodeInfo tree into a simplified UISnapshot.
 */
class AccessibilityParser {

    fun parse(root: AccessibilityNodeInfo, width: Int, height: Int): UISnapshot {
        val elements = mutableMapOf<Int, UIElement>()
        // Use a counter for IDs to keep them small and distinct from hashCodes
        var idCounter = 1

        fun traverse(node: AccessibilityNodeInfo, parentId: Int?): Int {
            val elementId = idCounter++
            
            // Capture basic properties
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val children = mutableListOf<Int>()
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                
                // Filter invisible nodes to reduce noise
                val isVisible = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    child.isVisibleToUser
                } else {
                    // Fallback for older APIs: Assume visible if bounds are not empty/offscreen
                    val childBounds = Rect()
                    child.getBoundsInScreen(childBounds)
                    childBounds.width() > 0 && childBounds.height() > 0 
                        && childBounds.intersect(0, 0, width, height)
                }

                if (isVisible) {
                    val childId = traverse(child, elementId)
                    children.add(childId)
                }
                
                // Important: Recycle the child node as we are done with it
                child.recycle()
            }

            val element = UIElement(
                id = elementId,
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                viewIdResourceName = node.viewIdResourceName,
                bounds = bounds,
                isClickable = node.isClickable,
                isEditable = node.isEditable,
                isScrollable = node.isScrollable,
                childrenIds = children,
                parentId = parentId
            )
            
            elements[elementId] = element
            return elementId
        }

        val rootId = traverse(root, null)

        return UISnapshot(
            rootId = rootId,
            elements = elements,
            screenWidth = width,
            screenHeight = height
        )
    }
}
