
package com.buzbuz.smartautoclicker.feature.dumb.config.domain

import android.content.Context
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getClickPressDurationConfig
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getClickRepeatCountConfig
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getClickRepeatDelayConfig
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getDumbConfigPreferences
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getPauseDurationConfig
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getSwipeDurationConfig
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getSwipeRepeatCountConfig
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getSwipeRepeatDelayConfig

internal fun Context.getDefaultDumbClickName(): String =
    getString(R.string.default_dumb_click_name)

internal fun Context.getDefaultDumbClickDurationMs(): Long = getDumbConfigPreferences()
    .getClickPressDurationConfig(resources.getInteger(R.integer.default_dumb_click_press_duration).toLong())

internal fun Context.getDefaultDumbClickRepeatCount(): Int = getDumbConfigPreferences()
    .getClickRepeatCountConfig(1)

internal fun Context.getDefaultDumbClickRepeatDelay(): Long = getDumbConfigPreferences()
    .getClickRepeatDelayConfig(0)

internal fun Context.getDefaultDumbSwipeName(): String =
    getString(R.string.default_dumb_swipe_name)

internal fun Context.getDefaultDumbSwipeDurationMs(): Long = getDumbConfigPreferences()
    .getSwipeDurationConfig(resources.getInteger(R.integer.default_dumb_swipe_duration).toLong())

internal fun Context.getDefaultDumbSwipeRepeatCount(): Int = getDumbConfigPreferences()
    .getSwipeRepeatCountConfig(1)

internal fun Context.getDefaultDumbSwipeRepeatDelay(): Long = getDumbConfigPreferences()
    .getSwipeRepeatDelayConfig(0)

internal fun Context.getDefaultDumbPauseName(): String =
    getString(R.string.default_dumb_pause_name)

internal fun Context.getDefaultDumbPauseDurationMs(): Long = getDumbConfigPreferences()
    .getPauseDurationConfig(resources.getInteger(R.integer.default_dumb_pause_duration).toLong())
