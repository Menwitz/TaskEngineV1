/*
 * Copyright (C) 2024 Kevin Buzeau
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.taskengine.core.database

import androidx.room.AutoMigration
import androidx.room.Database

import com.buzbuz.taskengine.core.database.dao.TutorialDao
import com.buzbuz.taskengine.core.database.entity.ActionEntity
import com.buzbuz.taskengine.core.database.entity.ConditionEntity
import com.buzbuz.taskengine.core.database.entity.EventEntity
import com.buzbuz.taskengine.core.database.entity.EventToggleEntity
import com.buzbuz.taskengine.core.database.entity.IntentExtraEntity
import com.buzbuz.taskengine.core.database.entity.ScenarioEntity
import com.buzbuz.taskengine.core.database.entity.TutorialSuccessEntity
import javax.inject.Singleton

@Singleton
@Database(
    entities = [
        ActionEntity::class,
        EventEntity::class,
        ScenarioEntity::class,
        ConditionEntity::class,
        IntentExtraEntity::class,
        EventToggleEntity::class,
        TutorialSuccessEntity::class,
    ],
    version = TUTORIAL_DATABASE_VERSION,
    exportSchema = true,

)
abstract class TutorialDatabase : ScenarioDatabase() {

    abstract fun tutorialDao(): TutorialDao
}

/** Current version of the database. */
const val TUTORIAL_DATABASE_VERSION = CLICK_DATABASE_VERSION