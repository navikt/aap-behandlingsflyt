pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Cache for GitHub Package Registry, hvor de fleste av avhengighetene våre publiseres
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        mavenCentral()
        // Trengs av app og api for Confluent sine Avro/Kafka-avhengigheter
        maven("https://packages.confluent.io/maven/")
    }
}

rootProject.name = "behandlingsflyt"

include(
    "app",
    "behandlingsflyt",
    "api",
    "repository",
    "lib-test",
    "kontrakt",
    "docs"
)
