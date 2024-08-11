
package com.buzbuz.taskengine.core.base

import android.accessibilityservice.GestureDescription
import android.content.Intent

/** Execute the actions related to Android. */
interface AndroidExecutor {

    /** Execute the provided gesture. */
    suspend fun executeGesture(gestureDescription: GestureDescription)

    /** Start the activity defined by the provided intent. */
    fun executeStartActivity(intent: Intent)

    /** Send a broadcast defined by the provided intent. */
    fun executeSendBroadcast(intent: Intent)
}

/** The maximum supported duration for a gesture. This limitation comes from Android GestureStroke API.  */
const val GESTURE_DURATION_MAX_VALUE = 59_999L