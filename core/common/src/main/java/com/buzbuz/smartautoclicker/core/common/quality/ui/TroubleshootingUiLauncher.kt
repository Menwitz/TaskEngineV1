package com.buzbuz.smartautoclicker.core.common.quality.ui

import androidx.fragment.app.FragmentActivity

interface TroubleshootingUiLauncher {
    fun show(activity: FragmentActivity, onDismiss: () -> Unit)
}
