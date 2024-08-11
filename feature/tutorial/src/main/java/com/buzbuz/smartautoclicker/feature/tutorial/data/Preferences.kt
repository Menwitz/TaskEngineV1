
package com.buzbuz.smartautoclicker.feature.tutorial.data

import android.content.Context
import android.content.SharedPreferences

/** @return the shared preferences for the tutorial. */
internal fun Context.getTutorialPreferences(): SharedPreferences =
    getSharedPreferences(
        TUTORIAL_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

/** @return if the tutorial stop with volume down popup have been shown at least once to the user. */
internal fun SharedPreferences.isStopVolumeDownPopupAlreadyShown(): Boolean = getBoolean(
    PREF_TUTORIAL_STOP_VOL_DOWN_POPUP_SHOWN,
    false,
)

/** Save a new value for the stop with volume down popup shown. */
internal fun SharedPreferences.Editor.putStopVolumeDownPopupAlreadyShown(enabled: Boolean) : SharedPreferences.Editor =
    putBoolean(PREF_TUTORIAL_STOP_VOL_DOWN_POPUP_SHOWN, enabled)

/** Tutorial SharedPreference name. */
private const val TUTORIAL_PREFERENCES_NAME = "TutorialPreferences"
/** Tells if the stop with volume down button dialog have been shown at least once.  */
private const val PREF_TUTORIAL_STOP_VOL_DOWN_POPUP_SHOWN = "Tutorial_Stop_Volume_Down_Popup_Shown"