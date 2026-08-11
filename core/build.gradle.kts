import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.3.21"
    // Java library to make this module consumable by other build systems
    id("java-library")
    // Application plugin to build a runnable CLI distribution
    id("application")
    // Gradle Kover Plugin for coverage reports
    id("org.jetbrains.kotlinx.kover") version "0.9.9"
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    // Explicit Kotlin stdlib dependency (gradle.properties disables the default bundling)
    implementation(kotlin("stdlib"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.14.4")
}

// This module is pure Kotlin/JVM and intentionally has NO IntelliJ Platform dependencies.
kotlin {
    jvmToolchain(17)
}

// Application (CLI) configuration - read more: https://docs.gradle.org/current/userguide/application_plugin.html
application {
    mainClass = "com.github.nguyenphuc22.androidpackagerenamer.core.cli.MainKt"
    applicationName = "package-renamer-core"
}

tasks {
    test {
        useJUnitPlatform()
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
        verify {
            rule {
                // Enforce a minimum line coverage on the core module
                minBound(85)
            }
        }
    }
}
