pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        repositories {
            maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.4"
    id("com.possible-triangle.helper") version "1.3"
}

stonecutter {
    create(rootProject) {
        branch("common") {
            versions(
                "1.21.1",
                "1.21.2",
//                "1.21.3",
                "1.21.4",
                "1.21.5",
                "1.21.6",
//                "1.21.7",
//                "1.21.8",
                "1.21.9",
//                "1.21.10",
                "1.21.11",
                "26.1",
                "26.2"
            )
        }
        branch("fabric") {
            versions(
                "1.21.1",
                "1.21.2",
//                "1.21.3",
                "1.21.4",
                "1.21.5",
                "1.21.6",
//                "1.21.7",
//                "1.21.8",
                "1.21.9",
//                "1.21.10",
                "1.21.11"
            ).buildscript("mapped.build.gradle.kts")
            versions("26.1", "26.2")
        }
        branch("neoforge") {
            versions(
                "1.21.1",
                "1.21.2",
//                "1.21.3",
                "1.21.4",
                "1.21.5",
                "1.21.6",
//                "1.21.7",
//                "1.21.8",
                "1.21.9",
//                "1.21.10",
                "1.21.11",
                "26.1"
            )
        }
    }
}

rootProject.name = "imguimc"