import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    // IntelliJ Platform Gradle Plugin
    id("org.jetbrains.intellij.platform") version "2.18.1"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.5.0"
    // Gradle Kover Plugin
    id("org.jetbrains.kotlinx.kover") version "0.9.9"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // Core package-renaming engine (pure Kotlin, no IntelliJ dependency)
    implementation(project(":core"))

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        // IntelliJ IDEA Community Edition as the lowest supported baseline (2024.1)
        create(properties("platformType"), properties("platformVersion"))
        // Plugin Dependencies (bundled with IntelliJ IDEA)
        bundledPlugin("com.intellij.java")
        // Test Framework
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = properties("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = file("README.md").readText().lines().run {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            if (!containsAll(listOf(start, end))) {
                throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
            }
            subList(indexOf(start) + 1, indexOf(end))
        }.joinToString("\n").let { markdownToHTML(it) }

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }

    pluginVerification {
        ides {
            // Verify against the recommended set of IntelliJ IDEA releases
            recommended()
            // Verify binary compatibility with the latest IntelliJ IDEA release
            create(IntelliJPlatformType.IntellijIdea, "2026.2")
            // Also verify binary compatibility with the latest Android Studio release
            create(IntelliJPlatformType.AndroidStudio, "2026.1.3.7")
        }
    }

    publishing {
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first())
    }
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover
kover {
    reports {
        total {
            html {
                onCheck = true
                htmlDir = layout.buildDirectory.dir("reports/kover/html")
            }
            xml {
                onCheck = true
                xmlFile = layout.buildDirectory.file("reports/kover/xml/report.xml")
            }
        }
    }
}

// Configure UI tests plugin
// Read more: https://github.com/JetBrains/intellij-ui-test-robot
val runIdeForUiTests = intellijPlatformTesting.runIde.register("runIdeForUiTests") {
    task {
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Drobot-server.port=8082",
                "-Dide.mac.message.dialogs.as.sheets=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
                "-Djb.consents.confirmation.enabled=false",
            )
        }
    }
    plugins {
        robotServerPlugin()
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    // Add coverage verification task
    register("verifyCoverage") {
        dependsOn("koverHtmlReport")
        doLast {
            val report = file("build/reports/kover/html/index.html")
            if (report.exists()) {
                val content = report.readText()

                // Extract class coverage percentage
                val classRegex = Regex("""<span class="percent">\s*(\d+\.?\d*)%\s*</span>\s*<span class="absValue">\s*\(\d+/\d+\)\s*</span>\s*</td>\s*<td class="coverageStat">""")
                val classMatch = classRegex.find(content)
                val classCoverage = classMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

                println("Current class coverage: $classCoverage%")

                if (classCoverage < 80.0) {
                    throw GradleException("Class coverage $classCoverage% is below minimum threshold of 80%")
                }

                println("Coverage verification passed!")
            }
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }
}
