
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.DialogChoice
import com.buzbuz.smartautoclicker.feature.dumb.config.R


/** Choices for the dumb action type selection dialog. */
sealed class DumbActionTypeChoice(
    title: Int,
    description: Int,
    iconId: Int?,
): DialogChoice(
    title = title,
    description = description,
    iconId = iconId,
) {
    /** Copy Action choice. */
    data object Copy : DumbActionTypeChoice(
        R.string.item_title_dumb_action_copy,
        R.string.item_desc_dumb_action_copy,
        R.drawable.ic_copy,
    )

    /** Click Action choice. */
    data object Click : DumbActionTypeChoice(
        R.string.item_title_dumb_click,
        R.string.item_desc_dumb_click,
        R.drawable.ic_click,
    )
    /** Swipe Action choice. */
    data object Swipe : DumbActionTypeChoice(
        R.string.item_title_dumb_swipe,
        R.string.item_desc_dumb_swipe,
        R.drawable.ic_swipe,
    )
    /** Pause Action choice. */
    data object Pause : DumbActionTypeChoice(
        R.string.item_title_dumb_pause,
        R.string.item_desc_dumb_pause,
        R.drawable.ic_wait,
    )
}

fun allDumbActionChoices() = listOf(
    DumbActionTypeChoice.Copy,
    DumbActionTypeChoice.Click,
    DumbActionTypeChoice.Swipe,
    DumbActionTypeChoice.Pause,
)