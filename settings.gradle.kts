pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // GeckoView is only published to Mozilla's Maven repository.
        maven {
            url = uri("https://maven.mozilla.org/maven2/")
            content {
                includeGroup("org.mozilla.geckoview")
            }
        }
    }
}

rootProject.name = "Ora"
include(":app")
 