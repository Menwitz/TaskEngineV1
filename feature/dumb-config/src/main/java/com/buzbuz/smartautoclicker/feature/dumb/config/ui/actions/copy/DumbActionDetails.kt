
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.copy

import android.content.Context

import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.Repeatable
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.dumb.config.R

/**
 * Action item.
 * @param icon the icon for the action.
 * @param name the name of the action.
 * @param detailsText the details for the action.
 * @param action the action represented by this item.
 */
data class DumbActionDetails (
    @DrawableRes val icon: Int,
    val name: String,
    val detailsText: String,
    val repeatCountText: String?,
    val haveError: Boolean,
    val action: DumbAction,
)

/** DiffUtil Callback comparing two ActionItem when updating the [DumbActionListAdapter] list. */
object DumbActionDiffUtilCallback: DiffUtil.ItemCallback<DumbActionDetails>() {

    override fun areItemsTheSame(
        oldItem: DumbActionDetails,
        newItem: DumbActionDetails,
    ): Boolean = oldItem.action.id == newItem.action.id

    override fun areContentsTheSame(
        oldItem: DumbActionDetails,
        newItem: DumbActionDetails,
    ): Boolean = oldItem == newItem
}

/** @return the [DumbActionDetails] corresponding to this action. */
fun DumbAction.toDumbActionDetails(
    context: Context,
    withPositions: Boolean = true,
    inError: Boolean = !isValid(),
): DumbActionDetails =
    when (this) {
        is DumbAction.DumbClick -> toClickDetails(context, withPositions, inError)
        is DumbAction.DumbSwipe -> toSwipeDetails(context, withPositions, inError)
        is DumbAction.DumbPause -> toPauseDetails(context, withPositions, inError)
        else -> throw IllegalArgumentException("Not yet supported")
    }

private fun DumbAction.DumbClick.toClickDetails(context: Context, withPositions: Boolean, inError: Boolean): DumbActionDetails =
    DumbActionDetails(
        icon = R.drawable.ic_click,
        name = name,
        detailsText = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            withPositions -> context.getString(
                R.string.item_desc_dumb_click_details,
                formatDuration(pressDurationMs), position.x, position.y,
            )
            else -> context.getString(
                R.string.item_desc_dumb_action_duration,
                formatDuration(pressDurationMs),
            )
        },
        repeatCountText = getRepeatDisplayText(context),
        haveError = inError,
        action = this,
    )

private fun DumbAction.DumbSwipe.toSwipeDetails(context: Context, withPositions: Boolean, inError: Boolean): DumbActionDetails =
    DumbActionDetails(
        icon = R.drawable.ic_swipe,
        name = name,
        detailsText = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            withPositions -> context.getString(
                R.string.item_desc_dumb_swipe_details,
                formatDuration(swipeDurationMs), fromPosition.x, fromPosition.y, toPosition.x, toPosition.y
            )
            else -> context.getString(
                R.string.item_desc_dumb_action_duration,
                formatDuration(swipeDurationMs),
            )
        },
        repeatCountText = getRepeatDisplayText(context),
        haveError = inError,
        action = this,
    )

private fun DumbAction.DumbPause.toPauseDetails(context: Context, withPositions: Boolean, inError: Boolean): DumbActionDetails =
    DumbActionDetails(
        icon = R.drawable.ic_wait,
        name = name,
        detailsText = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            withPositions -> context.getString(
                R.string.item_desc_dumb_pause_details,
                formatDuration(pauseDurationMs)
            )
            else -> context.getString(
                R.string.item_desc_dumb_action_duration,
                formatDuration(pauseDurationMs),
            )
        },
        repeatCountText = null,
        haveError = inError,
        action = this,
    )

private fun Repeatable.getRepeatDisplayText(context: Context): String =
    if (isRepeatInfinite) context.getString(R.string.item_desc_dumb_repeat_infinite)
    else context.getString(R.string.item_desc_dumb_repeat_count, repeatCount)