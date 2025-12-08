package com.buzbuz.smartautoclicker.core.ui.quality

import com.buzbuz.smartautoclicker.core.common.quality.ui.TroubleshootingUiLauncher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class QualityUiModule {

    @Binds
    abstract fun bindTroubleshootingUiLauncher(
        impl: TroubleshootingUiLauncherImpl
    ): TroubleshootingUiLauncher
}
