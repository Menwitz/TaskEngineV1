
package com.buzbuz.smartautoclicker.core.dumb.domain.model

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions

internal fun DumbScenarioWithActions.toDomain(asDomain: Boolean = false): DumbScenario =
    DumbScenario(
        id = Identifier(id = scenario.id, asTemporary = asDomain),
        name = scenario.name,
        repeatCount = scenario.repeatCount,
        isRepeatInfinite = scenario.isRepeatInfinite,
        maxDurationMin = scenario.maxDurationMin,
        isDurationInfinite = scenario.isDurationInfinite,
        randomize = scenario.randomize,
        dumbActions = dumbActions
            .sortedBy { it.priority }
            .map { dumbAction -> dumbAction.toDomain(asDomain) }
    )

internal fun DumbScenario.toEntity(): DumbScenarioEntity =
    DumbScenarioEntity(
        id = id.databaseId,
        name = name,
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        maxDurationMin = maxDurationMin,
        isDurationInfinite = isDurationInfinite,
        randomize = randomize,
    )