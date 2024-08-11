
package com.buzbuz.smartautoclicker.core.dumb.domain.model

import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

data class DumbScenario(
    override val id: Identifier,
    val name: String,
    val dumbActions: List<DumbAction> = emptyList(),
    override val repeatCount: Int,
    override val isRepeatInfinite: Boolean,
    val maxDurationMin: Int,
    val isDurationInfinite: Boolean,
    val randomize: Boolean,
) : Identifiable, Repeatable {

    fun isValid(): Boolean = name.isNotEmpty() && dumbActions.isNotEmpty()
}

const val DUMB_SCENARIO_MIN_DURATION_MINUTES = 1
const val DUMB_SCENARIO_MAX_DURATION_MINUTES = 1440
