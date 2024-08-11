
package com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules

import android.graphics.PointF
import android.graphics.Rect

import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

internal class OneStillTargetRules(highScore: Int) : BaseGameRules(highScore) {

    override fun onStart(area: Rect, targetSize: Int) {
        _targets.value = mapOf(
            TutorialGameTargetType.BLUE to PointF((area.width() - targetSize) / 2f, (area.height() - targetSize) / 2f),
        )
    }

    override fun onValidTargetHit(type: TutorialGameTargetType) {
        if (type != TutorialGameTargetType.BLUE) return
        score.value++
    }

}