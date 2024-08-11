
package com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF

import com.buzbuz.smartautoclicker.core.base.extensions.nextFloat
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

import kotlin.random.Random

internal class OneMovingTargetRules(highScore: Int) : BaseGameRules(highScore) {

    private val random: Random = Random(System.currentTimeMillis())

    private var targetsArea: RectF? = null

    override fun onStart(area: Rect, targetSize: Int) {
        targetsArea = RectF(
            area.left.toFloat() + TARGET_MARGIN,
            area.top.toFloat() + TARGET_MARGIN,
            area.right.toFloat() - targetSize - TARGET_MARGIN,
            area.bottom.toFloat() - targetSize - TARGET_MARGIN,
        )

        updateTargetPosition()
    }

    override fun onValidTargetHit(type: TutorialGameTargetType) {
        if (type != TutorialGameTargetType.BLUE) return

        score.value++
        updateTargetPosition()
    }

    private fun updateTargetPosition() {
        val area = targetsArea ?: return

        _targets.value = mapOf(
            TutorialGameTargetType.BLUE to PointF(
                random.nextFloat(area.left, area.right),
                random.nextFloat(area.top, area.bottom),
            ),
        )
    }
}