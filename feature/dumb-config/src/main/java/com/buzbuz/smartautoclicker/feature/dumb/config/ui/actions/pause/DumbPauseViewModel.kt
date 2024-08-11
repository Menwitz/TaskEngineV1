
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.pause

import android.content.Context

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.TimeUnitDropDownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.findAppropriateTimeUnit
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.formatDuration
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.toDurationMs
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getDumbConfigPreferences
import com.buzbuz.smartautoclicker.feature.dumb.config.data.putPauseDurationConfig

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import javax.inject.Inject

class DumbPauseViewModel @Inject constructor() : ViewModel() {

    private val _editedDumbPause: MutableStateFlow<DumbAction.DumbPause?> = MutableStateFlow(null)
    private val editedDumbPause: Flow<DumbAction.DumbPause> = _editedDumbPause.filterNotNull()

    /** Tells if the configured dumb pause is valid and can be saved. */
    val isValidDumbPause: Flow<Boolean> = _editedDumbPause
        .map { it != null && it.isValid() }

    /** The name of the pause. */
    val name: Flow<String> = editedDumbPause
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = editedDumbPause
        .map { it.name.isEmpty() }

    private val _selectedUnitItem: MutableStateFlow<TimeUnitDropDownItem> =
        MutableStateFlow(TimeUnitDropDownItem.Milliseconds)
    val selectedUnitItem: Flow<TimeUnitDropDownItem> = _selectedUnitItem

    /** The duration of the pause. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pauseDuration: Flow<String> = selectedUnitItem
        .flatMapLatest { unitItem ->
            editedDumbPause
                .map { unitItem.formatDuration(it.pauseDurationMs) }
                .take(1)
        }
    /** Tells if the press duration value is valid or not. */
    val pauseDurationError: Flow<Boolean> = editedDumbPause
        .map { it.pauseDurationMs <= 0 }

    fun setEditedDumbPause(pause: DumbAction.DumbPause) {
        _selectedUnitItem.value = pause.pauseDurationMs.findAppropriateTimeUnit()
        _editedDumbPause.value = pause.copy()
    }

    fun getEditedDumbPause(): DumbAction.DumbPause? =
        _editedDumbPause.value

    fun setName(newName: String) {
        _editedDumbPause.value = _editedDumbPause.value?.copy(name = newName)
    }

    fun setPauseDurationMs(duration: Long) {
        _editedDumbPause.value = _editedDumbPause.value?.let { oldValue ->
            val newDurationMs = duration.toDurationMs(_selectedUnitItem.value)

            if (oldValue.pauseDurationMs == newDurationMs) return
            oldValue.copy(pauseDurationMs = newDurationMs)
        }
    }

    fun setTimeUnit(unit: DropdownItem) {
        _selectedUnitItem.value = unit as? TimeUnitDropDownItem ?: TimeUnitDropDownItem.Milliseconds
    }

    fun saveLastConfig(context: Context) {
        _editedDumbPause.value?.let { pause ->
            context.getDumbConfigPreferences()
                .edit()
                .putPauseDurationConfig(pause.pauseDurationMs)
                .apply()
        }
    }
}