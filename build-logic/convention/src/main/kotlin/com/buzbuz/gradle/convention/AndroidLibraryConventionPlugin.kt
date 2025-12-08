
package com.buzbuz.gradle.convention

import com.buzbuz.gradle.core.androidLib
import com.buzbuz.gradle.core.libs.getLibs
import com.buzbuz.gradle.core.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        val libs = getLibs()

        plugins {
            apply(libs.plugins.android.library)
            apply(libs.plugins.jetBrains.kotlin.android)
        }

        androidLib {
            compileSdk = libs.versions.android.compileSdk

            defaultConfig.apply {
                targetSdk = libs.versions.android.targetSdk
                minSdk = libs.versions.android.minSdk
            }

            compileOptions.apply {
                sourceCompatibility = libs.versions.java
                targetCompatibility = libs.versions.java
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(libs.versions.jvmTargetEnum)
            }
        }
    }
}
