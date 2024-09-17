plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "behandlingflyt"

include(
    "app",
    "sakogbehandling",
    "faktagrunnlag",
    "verdityper",
    "tidslinje",
    "dbflyway",
    "dbtestdata",
    "httpklient",
    "lib-test",
    "tilgang",
    "pip"
)
