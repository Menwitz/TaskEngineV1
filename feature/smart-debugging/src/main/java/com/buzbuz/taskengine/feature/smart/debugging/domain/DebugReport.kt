
package com.buzbuz.taskengine.feature.smart.debugging.domain

import com.buzbuz.taskengine.core.domain.model.condition.ImageCondition
import com.buzbuz.taskengine.core.domain.model.event.ImageEvent
import com.buzbuz.taskengine.core.domain.model.scenario.Scenario

data class DebugReport internal constructor(
    val scenario: Scenario,
    val sessionInfo: ProcessingDebugInfo,
    val imageProcessedInfo: ProcessingDebugInfo,
    val eventsTriggeredCount: Long,
    val eventsProcessedInfo: List<Pair<ImageEvent, ProcessingDebugInfo>>,
    val conditionsDetectedCount: Long,
    val conditionsProcessedInfo: Map<Long, Pair<ImageCondition, ConditionProcessingDebugInfo>>,
)
