package com.buzbuz.smartautoclicker.core.ui.quality

import androidx.fragment.app.FragmentActivity
import com.buzbuz.smartautoclicker.core.common.quality.ui.TroubleshootingUiLauncher
import com.buzbuz.smartautoclicker.core.ui.quality.AccessibilityTroubleshootingDialog.Companion.FRAGMENT_RESULT_KEY_TROUBLESHOOTING
import com.buzbuz.smartautoclicker.core.ui.quality.AccessibilityTroubleshootingDialog.Companion.FRAGMENT_TAG_TROUBLESHOOTING_DIALOG
import javax.inject.Inject

class TroubleshootingUiLauncherImpl @Inject constructor() : TroubleshootingUiLauncher {
    override fun show(activity: FragmentActivity, onDismiss: () -> Unit) {
        activity.supportFragmentManager
            .setFragmentResultListener(FRAGMENT_RESULT_KEY_TROUBLESHOOTING, activity) { _, _ -> onDismiss() }
        AccessibilityTroubleshootingDialog().show(activity.supportFragmentManager, FRAGMENT_TAG_TROUBLESHOOTING_DIALOG)
    }
}
