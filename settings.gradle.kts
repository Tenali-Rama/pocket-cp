pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PocketCP"

include(":app")
include(":core:adaptive")
include(":core:codeforces")
include(":core:designsystem")
include(":core:network")
include(":core:screenshot")
include(":core:testing")
