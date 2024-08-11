
package com.buzbuz.taskengine.activity.list

import com.buzbuz.taskengine.core.domain.model.scenario.Scenario

data class ScenarioBackupSelection(
    val dumbSelection: Set<Long> = emptySet(),
    val smartSelection: Set<Long> = emptySet(),
)

fun ScenarioBackupSelection.isEmpty(): Boolean =
    dumbSelection.isEmpty() && smartSelection.isEmpty()

/**
 * Toggle the selected for backup state of a smart scenario.
 * @param item the smart scenario to be toggled.
 */
fun ScenarioBackupSelection.toggleScenarioSelectionForBackup(item: ScenarioListUiState.Item): ScenarioBackupSelection? =
    when (item) {
        is ScenarioListUiState.Item.Valid.Smart -> toggleSmartScenarioSelectionForBackup(item.scenario)
        else -> null
    }

fun ScenarioBackupSelection.toggleAllScenarioSelectionForBackup(allItems: List<ScenarioListUiState.Item>): ScenarioBackupSelection =
    if (dumbSelection.isNotEmpty() || smartSelection.isNotEmpty()) {
        copy(dumbSelection = emptySet(), smartSelection = emptySet())
    } else {
        val dumbIds = mutableSetOf<Long>()
        val smartIds = mutableSetOf<Long>()

        allItems.forEach { item ->
            when (item) {
                is ScenarioListUiState.Item.Valid.Smart -> smartIds.add(item.scenario.id.databaseId)
                else -> Unit
            }
        }

        copy(dumbSelection = dumbIds, smartSelection = smartIds)
    }

/**
 * Toggle the selected for backup state of a smart scenario.
 * @param scenario the smart scenario to be toggled.
 */
private fun ScenarioBackupSelection.toggleSmartScenarioSelectionForBackup(scenario: Scenario): ScenarioBackupSelection? {
    if (scenario.eventCount == 0) return null

    val newSelection = smartSelection.toMutableSet().apply {
        if (contains(scenario.id.databaseId)) remove(scenario.id.databaseId)
        else add(scenario.id.databaseId)
    }

    return copy(smartSelection = newSelection)
}

