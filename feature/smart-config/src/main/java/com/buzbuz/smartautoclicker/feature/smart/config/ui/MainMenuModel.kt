
package com.buzbuz.smartautoclicker.feature.smart.config.ui

import android.content.Context
import android.util.Log
import android.view.View

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject

/** View model for the [MainMenu]. */
class MainMenuModel @Inject constructor(
    private val detectionRepository: DetectionRepository,
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    private val scenarioDbId: StateFlow<Long?> = detectionRepository.scenarioId
        .map { it?.databaseId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )





    val isMediaProjectionStarted: StateFlow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.RECORDING || it == DetectionState.DETECTING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Tells if the scenario can be started. Edited scenario must be synchronized and engine should allow it. */
    val isStartButtonEnabled: Flow<Boolean> = combine(
        detectionRepository.canStartDetection,
        editionRepository.isEditionSynchronized,
        isMediaProjectionStarted
    ) { canStartDetection, isSynchronized, isProjectionStarted ->
        (canStartDetection || !isProjectionStarted) && isSynchronized
    }

    /** Tells if the detector can't work due to a native library load error. */
    val nativeLibError: Flow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.ERROR_NO_NATIVE_LIB }
        .distinctUntilChanged()

    private val _showAgentPrompt = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showAgentPrompt: Flow<Unit> = _showAgentPrompt

    /** Combined state of Detection OR Agent Running */
    val detectionState: StateFlow<UiState> = combine(
        detectionRepository.detectionState,
        detectionRepository.isAgentRunning
    ) { detState, isAgent ->
        when {
            isAgent -> UiState.Detecting
            detState == DetectionState.DETECTING -> UiState.Detecting
            else -> UiState.Idle
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        UiState.Idle,
    )

    /** Start/Stop the detection. */
    fun toggleDetection(context: Context) {
        viewModelScope.launch {
            if (detectionRepository.isAgentRunning.first()) {
                detectionRepository.stopAgent()
                return@launch
            }
            
            when (detectionState.value) {
                UiState.Detecting -> stopDetection()
                UiState.Idle -> {
                    if (scenarioDbId.value == -1L) {
                         _showAgentPrompt.tryEmit(Unit)
                    } else {
                        startDetection(context)
                    }
                }
            }
        }
    }

    /** Stop the detection. Returns true if it was started, false if not. */
    fun stopDetection(): Boolean {
        // Stop Agent if running
        // Note: checking synchronous value is hard with Flow, launching coroutine.
        // But for "Back Key" handling, we need immediate return.
        // We rely on detectionState.value which is StateFlow.
        
        if (detectionState.value !is UiState.Detecting) return false

        // We trigger both stops to be safe, or check flow?
        // Let's just blindly stop both.
        viewModelScope.launch {
            detectionRepository.stopAgent()
            detectionRepository.stopDetection() // This is synchronous in Repo usually?
        }
        return true
    }

    private fun startDetection(context: Context) {
        viewModelScope.launch {
            detectionRepository.startDetection(context, progressListener = null)
        }
    }
    
    fun startAgent(goal: String) {
        detectionRepository.startAgent(goal)
    }

    fun startScenarioEdition(onEditionStarted: () -> Unit) {
        scenarioDbId.value?.let { scenarioDatabaseId ->
            viewModelScope.launch(Dispatchers.IO) {
                if (editionRepository.startEdition(scenarioDatabaseId)) {
                    withContext(Dispatchers.Main) { onEditionStarted() }
                }
            }
        }
    }

    /** Save the configured scenario in the database. */
    fun saveScenarioChanges(onCompleted: (success: Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = editionRepository.saveEditions()

            withContext(Dispatchers.Main) {
                onCompleted(result)
            }
        }
    }

    /** Cancel all changes made by the user. */
    fun cancelScenarioChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            editionRepository.stopEdition()
        }
    }

    fun monitorViews(playMenuButton: View, configMenuButton: View) {
        monitoredViewsManager.apply {
            attach(MonitoredViewType.MAIN_MENU_BUTTON_PLAY, playMenuButton, ViewPositioningType.SCREEN)
            attach(MonitoredViewType.MAIN_MENU_BUTTON_CONFIG, configMenuButton, ViewPositioningType.SCREEN)
        }
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.MAIN_MENU_BUTTON_PLAY)
            detach(MonitoredViewType.MAIN_MENU_BUTTON_CONFIG)
        }
    }

    fun shouldRestartMediaProjection(): Boolean =
        !isMediaProjectionStarted.value

}

sealed class UiState {
    data object Detecting: UiState()
    data object Idle: UiState()
}

private const val TAG = "MainMenuViewModel"