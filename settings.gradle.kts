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
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:model")
include(":core:navigation")
include(":core:network")
include(":core:screenshot")
include(":core:testing")
