package com.buzbuz.smartautoclicker.feature.backup.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.buzbuz.smartautoclicker.R

/**
 * Lightweight placeholder dialog shown when the legacy backup/import feature is requested.
 */
class BackupDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.backup_disabled_title)
            .setMessage(R.string.backup_disabled_message)
            .setPositiveButton(android.R.string.ok, null)
            .create()

    companion object {
        const val FRAGMENT_TAG_BACKUP_DIALOG = "backup_dialog"

        fun newInstance(
            @Suppress("UNUSED_PARAMETER") isImport: Boolean,
            @Suppress("UNUSED_PARAMETER") smartScenariosToBackup: Collection<Long>? = null,
        ): BackupDialogFragment = BackupDialogFragment()
    }
}

