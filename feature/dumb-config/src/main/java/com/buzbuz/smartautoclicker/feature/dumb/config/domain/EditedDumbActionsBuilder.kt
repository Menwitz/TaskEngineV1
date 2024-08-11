
package com.buzbuz.smartautoclicker.feature.dumb.config.domain

import android.content.Context
import android.graphics.Point

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.identifier.IdentifierCreator
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction

class EditedDumbActionsBuilder {

    private val dumbActionsIdCreator = IdentifierCreator()

    private var dumbScenarioId: Identifier? = null

    internal fun startEdition(scenarioId: Identifier) {
        dumbActionsIdCreator.resetIdCount()
        dumbScenarioId = scenarioId
    }

    internal fun clearState() {
        dumbActionsIdCreator.resetIdCount()
        dumbScenarioId = null
    }

    fun createNewDumbClick(context: Context, position: Point): DumbAction.DumbClick =
        DumbAction.DumbClick(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbClickName(),
            position = position,
            pressDurationMs = context.getDefaultDumbClickDurationMs(),
            repeatCount = context.getDefaultDumbClickRepeatCount(),
            isRepeatInfinite = false,
            repeatDelayMs = context.getDefaultDumbClickRepeatDelay(),
        )

    fun createNewDumbSwipe(context: Context, from: Point, to: Point): DumbAction.DumbSwipe =
        DumbAction.DumbSwipe(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbSwipeName(),
            fromPosition = from,
            toPosition = to,
            swipeDurationMs = context.getDefaultDumbSwipeDurationMs(),
            repeatCount = context.getDefaultDumbSwipeRepeatCount(),
            isRepeatInfinite = false,
            repeatDelayMs = context.getDefaultDumbSwipeRepeatDelay(),
        )

    fun createNewDumbPause(context: Context): DumbAction.DumbPause =
        DumbAction.DumbPause(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbPauseName(),
            pauseDurationMs = context.getDefaultDumbPauseDurationMs(),
        )

    fun createNewDumbActionFrom(from: DumbAction): DumbAction =
        when (from) {
            is DumbAction.DumbClick -> from.copy(
                id = dumbActionsIdCreator.generateNewIdentifier(),
                scenarioId = getEditedScenarioIdOrThrow(),
            )
            is DumbAction.DumbSwipe -> from.copy(
                id = dumbActionsIdCreator.generateNewIdentifier(),
                scenarioId = getEditedScenarioIdOrThrow(),
            )
            is DumbAction.DumbPause -> from.copy(
                id = dumbActionsIdCreator.generateNewIdentifier(),
                scenarioId = getEditedScenarioIdOrThrow(),
            )
        }

    private fun getEditedScenarioIdOrThrow(): Identifier = dumbScenarioId
        ?: throw IllegalStateException("Can't create items without an edited dumb scenario")
}