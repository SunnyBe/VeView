import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.kotlin.dsl.withType

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.arturbosch.detekt) apply true
    alias(libs.plugins.gradle.ktlint) apply true
}

val detektFormatting = libs.detekt.formatting
val detektPlugin = libs.plugins.arturbosch.detekt
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    detekt {
        buildUponDefaultConfig = true
        allRules = false // activate all available (even unstable) rules.
        config.setFrom("${rootProject.file("detekt.yml")}")
    }

    dependencies {
        detektPlugins(detektFormatting)
        detektPlugins("io.nlopez.compose.rules:detekt:0.4.23")
    }

    tasks.withType<Detekt>().configureEach {
        reports {
            html.required.set(false)
            xml.required.set(false)
            txt.required.set(false)
            sarif.required.set(true)
        }
    }
}

dependencyLocking {
    lockAllConfigurations()
    lockFile = file("$projectDir/dependency-locks/app.lockfile")
    lockMode = LockMode.STRICT
}
