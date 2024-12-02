plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "behandlingflyt"

include(
    "app",
    "sakogbehandling",
    "faktagrunnlag",
    "dbflyway",
    "integrasjon",
    "lib-test",
    "kontrakt",
    "pip"
)
