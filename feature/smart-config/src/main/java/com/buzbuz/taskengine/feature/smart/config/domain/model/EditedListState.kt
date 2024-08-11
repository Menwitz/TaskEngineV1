
package com.buzbuz.taskengine.feature.smart.config.domain.model

data class EditedListState<EditedType>(
    val value: List<EditedType>?,
    val itemValidity: List<Boolean>,
    val hasChanged: Boolean,
    val canBeSaved: Boolean,
)