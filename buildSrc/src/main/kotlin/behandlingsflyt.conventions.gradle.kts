import kotlin.math.max

// Felles kode for alle build.gradle.kts filer som laster inn denne conventions pluginen

plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "no.nav.aap"
version = project.findProperty("version")?.toString() ?: "0.0.0"

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    mavenLocal()
}

// https://docs.gradle.org/8.12.1/userguide/jvm_test_suite_plugin.html
testing {
    suites {
        @Suppress("UnstableApiUsage") val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

private fun bestemAntallTestTråder(): Int {
    val isCiBuild =
        providers.environmentVariable("CI").isPresent || providers.environmentVariable("GITHUB_ACTIONS").isPresent
    val processors = Runtime.getRuntime().availableProcessors()
    val antallTråder =
        if (isCiBuild) {
            (processors * 1.5).toInt() // vi har mye io-wait under testene våre
        } else {
            // reduser antall tråder ved lokal kjøring for å unngå at utvikler-maskinen blir for treg
            max(processors / 2, processors - 4)
        }

    logger.quiet("Bruker opptil ${antallTråder} tråder for testkjøring ($processors kjerner tilgjengelig)")
    return antallTråder
}

tasks {
    test {
        useJUnitPlatform()
        maxParallelForks = bestemAntallTestTråder()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    (findByName("distTar") as? Tar)?.apply {
        // Bruk et unikt navn for jar-filen til distTar, for å unngå navnekollisjoner i multi-modul prosjekt,
        // slik at vi ikke bruker samme navn, feks. "kontrakt.jar" "api.jar" i flere moduler.
        // Dette unngår feil av typen "Entry <name>.jar is a duplicate but no duplicate handling strategy has been set"
        // Alternativet er å unngå å bruke det eksakt samme navnet på moduler i forskjellige prosjekter, som feks "kontrakt".
        archiveBaseName.set("${rootProject.name}-${project.name}")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)

        // Bruk et unikt navn for <project>.kotlin_module for hvert sub-prosjekt,
        // slik at vi unngår navnekollisjoner når vi inkluderer flere av våre kotlin-moduler i samme jar-fil, feks. ved bruk av shadowJar.
        // Kroneksempelet er "kontrakt.kotlin_module" fra både behandlingsflyt, brev, meldekort og andre steder.
        // Dette gjør at vi kan beholde informasjonen for hver modul, og kotlin-reflect og andre verktøy fungerer som forventet.
        // Alternativet er å unngå å bruke det eksakt samme navnet på moduler i forskjellige prosjekter, som feks "kontrakt".
        freeCompilerArgs.add("-module-name=${rootProject.name}-${project.name}")
    }
}

// Pass på at når vi kaller JavaExec eller Test tasks så bruker vi samme språk-versjon som vi kompilerer til
val toolchainLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}
tasks.withType<Test>().configureEach { javaLauncher.set(toolchainLauncher) }
tasks.withType<JavaExec>().configureEach { javaLauncher.set(toolchainLauncher) }


kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")