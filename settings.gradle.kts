plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    "app",
    "behandlingsflyt",
    "api",
    "repository",
    "lib-test",
    "kontrakt"
)

dependencyResolutionManagement {
    // Felles for alle gradle prosjekter i repoet
    repositories {
        mavenCentral()
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/behandlingsflyt")
            credentials {
                username = "x-access-token"
                password = (System.getenv("GITHUB_PASSWORD")
                    ?: System.getenv("GITHUB_TOKEN")
                    ?: error("GITHUB_TOKEN not set"))
            }
        }
    }
}
