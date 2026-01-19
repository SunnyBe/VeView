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
    alias(libs.plugins.ksp) apply false
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

tasks.register("releaseLocal") {
    group = "Check SDK release"
    description = "Run assemble release and generates aar for us to see what is compiled."

    val clientProject = subprojects.firstOrNull { it.name == "app" }
        ?: error("app module does not exist.")

    val sdkProject = subprojects.firstOrNull { it.name == "veview-sdk" }
        ?: error("veview-sdk does not exist")

    dependsOn("${sdkProject.name}:assembleRelease") // generate release aar for sdk

    doLast {
        val sdkVersion = sdkProject.version.toString().takeIf { it != "unspecified" } ?: "local"
        val aarFile = sdkProject.layout.buildDirectory.file("outputs/aar/veview-sdk-release.aar").get().asFile

        check(aarFile.isFile) { "AAR file not found at ${aarFile.path}" }

        copy {
            from(aarFile)
            into(file("${clientProject.projectDir}/libs"))
            rename { "veview-sdk-release.$sdkVersion.aar" }
        }

        println("Copied SDK AAR version $sdkVersion to app/libs")
    }
}
