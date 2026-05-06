pluginManagement {
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

rootProject.name = "Personal Finance Tracker"
include(":app")
include(":core:common")
project(":core:common").projectDir = File("core/common")

include(":core:data")
project(":core:data").projectDir = File("core/data")

include(":core:domain")
project(":core:domain").projectDir = File("core/domain")
