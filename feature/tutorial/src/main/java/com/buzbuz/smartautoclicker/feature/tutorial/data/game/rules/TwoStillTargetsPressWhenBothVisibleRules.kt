
package com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules

import android.graphics.PointF
import android.graphics.Rect

import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

internal class TwoStillTargetsPressWhenBothVisibleRules(highScore: Int) : BaseGameRules(highScore) {

    private var redTargetPosition: PointF? = null
    private var nextRedVisibilityToggleTimerValue: Int? = null

    override fun onStart(area: Rect, targetSize: Int) {
        redTargetPosition = PointF(area.width() - (targetSize * 1.5f), (area.height() - targetSize) / 2f)

        _targets.value = mapOf(
            TutorialGameTargetType.BLUE to PointF(targetSize / 2f, (area.height() - targetSize) / 2f),
        )
        toggleRedVisibility()
    }

    override fun onTimerTick(timeLeft: Int) {
        if (timeLeft != nextRedVisibilityToggleTimerValue) return
        toggleRedVisibility()
    }

    override fun onValidTargetHit(type: TutorialGameTargetType) {
        val blueIsVisible = _targets.value.containsKey(TutorialGameTargetType.BLUE)
        val redIsVisible = _targets.value.containsKey(TutorialGameTargetType.RED)

        if (type == TutorialGameTargetType.BLUE && blueIsVisible && redIsVisible) score.value++
        else score.value--
    }

    private fun toggleRedVisibility() {
        val redPosition = redTargetPosition ?: return

        _targets.value = _targets.value.toMutableMap().apply {
            if (containsKey(TutorialGameTargetType.RED)) remove(TutorialGameTargetType.RED)
            else put(TutorialGameTargetType.RED, redPosition)
        }
        nextRedVisibilityToggleTimerValue = timer.value - 1
    }
}