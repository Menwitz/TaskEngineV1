
package com.buzbuz.smartautoclicker.core.dumb.domain.model

import android.graphics.Point

import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionType

internal fun DumbActionEntity.toDomain(asDomain: Boolean = false): DumbAction = when (type) {
    DumbActionType.CLICK -> toDomainClick(asDomain)
    DumbActionType.SWIPE -> toDomainSwipe(asDomain)
    DumbActionType.PAUSE -> toDomainPause(asDomain)
}
internal fun DumbAction.toEntity(scenarioDbId: Long = DATABASE_ID_INSERTION): DumbActionEntity = when (this) {
    is DumbAction.DumbClick -> toClickEntity(scenarioDbId)
    is DumbAction.DumbSwipe -> toSwipeEntity(scenarioDbId)
    is DumbAction.DumbPause -> toPauseEntity(scenarioDbId)
}

private fun DumbActionEntity.toDomainClick(asDomain: Boolean): DumbAction.DumbClick =
    DumbAction.DumbClick(
        id = Identifier(id = id, asTemporary = asDomain),
        scenarioId = Identifier(id = dumbScenarioId, asTemporary = asDomain),
        name = name,
        priority = priority,
        position = Point(x!!, y!!),
        pressDurationMs = pressDuration!!,
        repeatCount = repeatCount!!,
        isRepeatInfinite = isRepeatInfinite!!,
        repeatDelayMs = repeatDelay!!,
    )

private fun DumbActionEntity.toDomainSwipe(asDomain: Boolean): DumbAction.DumbSwipe =
    DumbAction.DumbSwipe(
        id = Identifier(id = id, asTemporary = asDomain),
        scenarioId = Identifier(id = dumbScenarioId, asTemporary = asDomain),
        name = name,
        priority = priority,
        fromPosition = Point(fromX!!, fromY!!),
        toPosition = Point(toX!!, toY!!),
        swipeDurationMs = swipeDuration!!,
        repeatCount = repeatCount!!,
        isRepeatInfinite = isRepeatInfinite!!,
        repeatDelayMs = repeatDelay!!,
    )

private fun DumbActionEntity.toDomainPause(asDomain: Boolean): DumbAction.DumbPause =
    DumbAction.DumbPause(
        id = Identifier(id = id, asTemporary = asDomain),
        scenarioId = Identifier(id = dumbScenarioId, asTemporary = asDomain),
        name = name,
        priority = priority,
        pauseDurationMs = pauseDuration!!,
    )

private fun DumbAction.DumbClick.toClickEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Click is incomplete.")

    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = if (scenarioDbId != DATABASE_ID_INSERTION) scenarioDbId else scenarioId.databaseId,
        name = name,
        priority = priority,
        type = DumbActionType.CLICK,
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        repeatDelay = repeatDelayMs,
        pressDuration = pressDurationMs,
        x = position.x,
        y = position.y,
    )
}

private fun DumbAction.DumbSwipe.toSwipeEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Swipe is incomplete.")

    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = if (scenarioDbId != DATABASE_ID_INSERTION) scenarioDbId else scenarioId.databaseId,
        name = name,
        priority = priority,
        type = DumbActionType.SWIPE,
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        repeatDelay = repeatDelayMs,
        swipeDuration = swipeDurationMs,
        fromX = fromPosition.x,
        fromY = fromPosition.y,
        toX = toPosition.x,
        toY = toPosition.y,
    )
}

private fun DumbAction.DumbPause.toPauseEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Pause is incomplete.")

    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = if (scenarioDbId != DATABASE_ID_INSERTION) scenarioDbId else scenarioId.databaseId,
        name = name,
        priority = priority,
        type = DumbActionType.PAUSE,
        pauseDuration = pauseDurationMs,
    )
}