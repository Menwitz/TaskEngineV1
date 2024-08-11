
package com.buzbuz.smartautoclicker.core.dumb.data.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import kotlinx.serialization.Serializable

/**
 * Entity defining a dumb scenario.
 *
 * A scenario has a relation "one to many" with [DumbActionEntity], which is represented
 * by [DumbScenarioWithActions].
 *
 * @param id the unique identifier for a scenario.
 * @param name the name of the scenario.
 * @param repeatCount
 * @param isRepeatInfinite
 * @param maxDurationMin
 * @param isDurationInfinite
 */
@Entity(tableName = "dumb_scenario_table")
@Serializable
data class DumbScenarioEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "repeat_count") val repeatCount: Int,
    @ColumnInfo(name = "is_repeat_infinite") val isRepeatInfinite: Boolean,
    @ColumnInfo(name = "max_duration_minutes") val maxDurationMin: Int,
    @ColumnInfo(name = "is_duration_infinite") val isDurationInfinite: Boolean,
    @ColumnInfo(name = "randomize") val randomize: Boolean,
) : EntityWithId

/**
 * Entity embedding a dumb scenario and its dumb actions.
 *
 * Automatically do the junction between dumb_scenario_table and dumb_action_table, and provide
 * this representation of the one to many relation between dumb scenario and dumb action entity.
 *
 * @param scenario the dumb scenario entity.
 * @param dumbActions the list of dumb actions entity for this scenario.
 */
@Serializable
data class DumbScenarioWithActions(
    @Embedded val scenario: DumbScenarioEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dumb_scenario_id"
    )
    val dumbActions: List<DumbActionEntity>
)