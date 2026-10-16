import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import kotlin.math.max
import kotlin.math.min

// Felles kode for alle build.gradle.kts filer som laster inn denne conventions pluginen

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("dev.detekt")
    `jvm-test-suite`
}

group = "no.nav.aap.behandlingsflyt"
version = project.findProperty("version")?.toString() ?: "0.0.0"

dependencies {
    dokkaPlugin("com.glureau:html-mermaid-dokka-plugin:0.6.0")
}

// https://docs.gradle.org/9.6.1/userguide/java_testing.html#java_testing
testing {
    suites {
        @Suppress("UnstableApiUsage")
        named<JvmTestSuite>("test") {
            targets.all {
                testTask {
                    environment("TZ", "Europe/Oslo")
                }
            }
            useJUnitJupiter()
        }
    }
}

dokka {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(VisibilityModifier.entries.toSet())
        sourceLink {
            remoteUrl("https://github.com/navikt/aap-behandlingsflyt/blob/main")
            localDirectory.set(rootDir)
        }
    }
    dokkaPublications {
        configureEach {
            suppressObviousFunctions.set(true)
            suppressInheritedMembers.set(false)
        }
    }
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
    buildUponDefaultConfig = false
}

tasks.withType(dev.detekt.gradle.Detekt::class.java).configureEach {
    ignoreFailures = true
    reports {
        html.required.set(true)
        checkstyle.required.set(false)
        sarif.required.set(true)
        markdown.required.set(false)
    }
}

tasks.named("check").configure {
    setDependsOn(dependsOn.filterNot {
        it.toString().contains("detekt", ignoreCase = true)
    })
}

private fun bestemAntallTestTråder(): Int {
    // Overstyr med -PtestForks=N, f.eks. når bare én modul testes på en maskin med mange kjerner
    providers.gradleProperty("testForks").orNull?.let { return it.toInt() }
    val isCiBuild =
        providers.environmentVariable("CI").isPresent || providers.environmentVariable("GITHUB_ACTIONS").isPresent
    val processors = Runtime.getRuntime().availableProcessors()
    val antallTråder =
        if (isCiBuild) {
            processors
        } else {
            /**
             * Begrens antall tråder ved lokal kjøring for å unngå at utvikler-maskinen blir for treg
             * Mer enn 6 tråder krasjer lokal kjøring for M3 Max
             */
            min(4, max(processors / 2, processors - 4))
        }
    return antallTråder
}

/**
 * Tagger på testklasser som trenger Docker (Testcontainers). Klasser uten disse taggene er kjernetester.
 * - db: Postgres via TestDataSource eller postgreSQLContainer()
 * - kafka: Kafka-container
 * - flyt: ende-til-ende-tester av flyten (AbstraktFlytOrkestratorTest og subklasser, også db)
 */
val dockerTagger = arrayOf("db", "kafka", "flyt")

/**
 * `./gradlew coreTest` (eller `:behandlingsflyt:coreTest`) kjører `test` uten testene som trenger Docker, og er ment
 * å kjøres etter hver endring. Den er et alias og ikke en egen Test-task, slik at IntelliJ bare ser `test` og ikke
 * spør hvilken task som skal kjøres når man kjører en enkelt test.
 */
val kjørerCoreTest = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "coreTest" }
if (kjørerCoreTest && gradle.startParameter.taskNames.any { it.substringAfterLast(':') in setOf("test", "check", "build") }) {
    throw GradleException("coreTest gjør om test til bare kjernetester, og kan ikke kjøres sammen med test, check eller build")
}

tasks {
    test {
        useJUnitPlatform()
        maxParallelForks = bestemAntallTestTråder()
        systemProperty("junit.jupiter.execution.timeout.default", "3m")
        testLogging {
            events("passed", "skipped", "failed")
        }
        // Ryuk (Testcontainers sin opprydder) feiler lokalt under last i Docker Desktop: testene får ikke kontakt med
        // den, eller den stopper containerne midt i kjøringen. Uten Ryuk stopper Testcontainers containerne når
        // test-JVM-en avslutter. CI beholder Ryuk; lokalt kan den skrus på igjen med TESTCONTAINERS_RYUK_DISABLED=false.
        val erCiBygg = providers.environmentVariable("CI").isPresent || providers.environmentVariable("GITHUB_ACTIONS").isPresent
        if (!erCiBygg) {
            environment(
                "TESTCONTAINERS_RYUK_DISABLED",
                providers.environmentVariable("TESTCONTAINERS_RYUK_DISABLED").getOrElse("true")
            )
        }
        if (kjørerCoreTest) {
            useJUnitPlatform {
                excludeTags(*dockerTagger)
            }
            // Får en test uten tag til å feile med en forklaring i stedet for å starte en container
            environment("TESTCONTAINERS_IMAGE_SUBSTITUTOR", "no.nav.aap.behandlingsflyt.test.DockerIkkeTillattIKjernetester")
        }
    }

    register("coreTest") {
        description = "Kjører testene som ikke trenger Docker (alt uten taggene ${dockerTagger.joinToString()})."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        dependsOn(test)
    }

    (findByName("jar") as? Jar)?.apply {
        // Bruk et unikt navn for jar-filen til hver submodul, for å unngå navnekollisjoner i multi-modul prosjekt,
        // gjennom at vi ikke bruker samme navn, feks. "kontrakt.jar" og "api.jar", i flere moduler.
        // Dette unngår feil av typen "Entry <name>.jar is a duplicate but no duplicate handling strategy has been set"
        // Alternativet er å unngå å bruke det eksakt samme navnet på submoduler fra forskjellige moduler,
        // som feks "kontrakt".
        archiveBaseName.set("${rootProject.name}-${project.name}")
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)

        // Bruk et unikt navn for <submodule>.kotlin_module for hver Gradle-submodul, for å unngå navnekollisjoner i
        // multi-modul prosjekt, hvor vi inkluderer flere av våre kotlin-moduler i samme jar-fil eller
        // på samme runtime classpath. Kroneksempelet er "kontrakt.kotlin_module" fra både behandlingsflyt, brev,
        // meldekort og andre steder.
        // Dette gjør at vi kan beholde informasjonen for hver kotlin_module, og kotlin-reflect og andre verktøy
        // fungerer som forventet. Alternativet er å unngå å bruke det eksakt samme navnet på submoduler fra
        // forskjellige moduler, som feks "kontrakt".
        moduleName.set("${rootProject.name}-${project.name}")
    }
}

// Pass på at når vi kaller JavaExec eller Test tasks så bruker vi samme språk-versjon som vi kompilerer til
val toolchainLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}
tasks.withType<Test>().configureEach { javaLauncher.set(toolchainLauncher) }
tasks.withType<JavaExec>().configureEach { javaLauncher.set(toolchainLauncher) }


kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")
