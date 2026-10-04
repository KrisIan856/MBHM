@Suppress("UNCHECKED_CAST")
try {
    val pe = Class.forName("java.lang.ProcessEnvironment")
    val envField = pe.getDeclaredField("theEnvironment")
    envField.isAccessible = true
    (envField.get(null) as? MutableMap<String, String>)?.remove("ANDROID_PREFS_ROOT")
    val ciEnvField = pe.getDeclaredField("theCaseInsensitiveEnvironment")
    ciEnvField.isAccessible = true
    (ciEnvField.get(null) as? MutableMap<String, String>)?.remove("ANDROID_PREFS_ROOT")
} catch (_: Throwable) {}

pluginManagement {
    repositories {
        google()
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
    }
}

rootProject.name = "MBHM"
include(":app")

