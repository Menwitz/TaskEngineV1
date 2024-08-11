
package com.buzbuz.taskengine.feature.smart.config.ui.common.bindings

import android.content.ComponentName
import com.buzbuz.taskengine.core.android.application.AndroidApplicationInfo

import com.buzbuz.taskengine.feature.smart.config.databinding.ItemApplicationBinding

/** Binds to the provided activity. */
fun ItemApplicationBinding.bind(activity: AndroidApplicationInfo, listener: ((ComponentName) -> Unit)? = null) {
    textApp.text = activity.name
    iconApp.setImageDrawable(activity.icon)

    listener?.let { root.setOnClickListener { it(activity.componentName) } }
}