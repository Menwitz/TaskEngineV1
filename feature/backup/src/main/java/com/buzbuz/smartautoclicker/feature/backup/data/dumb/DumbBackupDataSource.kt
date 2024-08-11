
package com.buzbuz.smartautoclicker.feature.backup.data.dumb

import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.core.dumb.data.database.DUMB_DATABASE_VERSION
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.feature.backup.data.base.SCENARIO_BACKUP_EXTENSION
import com.buzbuz.smartautoclicker.feature.backup.data.base.ScenarioBackupDataSource
import com.buzbuz.smartautoclicker.feature.backup.data.base.ScenarioBackupSerializer

import java.io.File

internal class DumbBackupDataSource(
    appDataDir: File,
): ScenarioBackupDataSource<DumbScenarioBackup, DumbScenarioWithActions>(appDataDir) {

    /**
     * Regex matching a condition file into its folder in a backup archive.
     * Will match any file like "scenarioId/Condition_randomNumber".
     *
     * You can try it out here: https://regex101.com
     */
    private val scenarioUnzipMatchRegex = """dumb-[0-9]+/[0-9]+$SCENARIO_BACKUP_EXTENSION"""
        .toRegex()

    override val serializer: ScenarioBackupSerializer<DumbScenarioBackup> = DumbScenarioSerializer()

    override fun isScenarioBackupFileZipEntry(fileName: String): Boolean =
        fileName.matches(scenarioUnzipMatchRegex)

    override fun isScenarioBackupAdditionalFileZipEntry(fileName: String): Boolean =
        false

    override fun getBackupZipFolderName(scenario: DumbScenarioWithActions): String =
        "dumb-${scenario.scenario.id}"

    override fun getBackupFileName(scenario: DumbScenarioWithActions): String =
        "${scenario.scenario.id}$SCENARIO_BACKUP_EXTENSION"

    override fun createBackupFromScenario(scenario: DumbScenarioWithActions, screenSize: Point): DumbScenarioBackup =
        DumbScenarioBackup(
            dumbScenario = scenario,
            screenWidth = screenSize.x,
            screenHeight = screenSize.y,
            version = DUMB_DATABASE_VERSION,
        )

    override fun verifyExtractedBackup(backup: DumbScenarioBackup, screenSize: Point): DumbScenarioWithActions? {
        if (backup.dumbScenario.dumbActions.isEmpty()) {
            Log.w(TAG, "Invalid dumb scenario, dumb action list is empty.")
            return null
        }

        return backup.dumbScenario
    }

    override fun getBackupAdditionalFilesPaths(scenario: DumbScenarioWithActions): Set<String> =
        emptySet()
}

/** Tag for logs. */
private const val TAG = "DumbBackupEngine"