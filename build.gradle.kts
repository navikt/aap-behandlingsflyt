// Kotlin konfigurasjonen er gitt av pluginen 'aap.conventions' i buildSrc
// og settings.gradle.kts

plugins {
    base
    `maven-publish`
    id("org.cyclonedx.bom") version "3.4.1"
    id("aap.conventions")
}

// Produser en SBOM (Software Bill of Materials) og last den opp som et Maven-artifact
group = "no.nav.aap.behandlingsflyt"
version = project.findProperty("version")?.toString() ?: "0.0.0"

tasks {
    cyclonedxBom {
        jsonOutput.unsetConvention() // ikke lag både json og xml
    }
}

// cyclonedxDirectBom-tasker resolver classpaths på tvers av subprojects. Vi kobler
// kompileringstaskene som produserer disse klassene inn som eksplisitte input-filer
// (resolvedDependencies er allerede annotert @InputFiles på tasken), i stedet for å
// bruke dependsOn, slik at Gradle forstår hvorfor avhengigheten trengs.
val allCompileTasks = subprojects.flatMap { sub ->
    sub.tasks.matching { it.name == "compileKotlin" || it.name == "compileJava" }
}
subprojects.forEach { sub ->
    sub.tasks.withType<org.cyclonedx.gradle.CyclonedxDirectTask>().configureEach {
        resolvedDependencies.from(allCompileTasks)
    }
}

publishing {
    publications {
        create<MavenPublication>("bomXml") {
            // Bruk flatMap for å unngå å kalle get() på task-provideren ved konfigurasjonstidspunkt;
            // dette holder koblingen mellom tasken og artifact-outputen lat.
            artifact(tasks.cyclonedxBom.flatMap { it.xmlOutput }) {
                extension = "xml"
            }
            artifactId = "sbom"
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/aap-behandlingsflyt")
            credentials {
                username = "x-access-token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// Call the tasks of the subprojects
for (taskName in listOf<String>("clean", "build", "assemble", "check")) {
    tasks.named(taskName) {
        dependsOn(subprojects.map { it.tasks.named(taskName) })
    }
}

// Merge Detekt reports from all subprojects
val detektReportMergeSarif = tasks.register<dev.detekt.gradle.report.ReportMergeTask>("detektReportMergeSarif") {
    group = "verification"
    description = "Merges Detekt SARIF reports from all subprojects into one file."
    output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

val detektProjectBaseline = tasks.register<dev.detekt.gradle.DetektCreateBaselineTask>("detektProjectBaseline") {
    group = "verification"
    description = "Overrides current baseline for all modules."
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(files(rootDir))
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/baseline.xml"))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/resources/**")
    exclude("**/build/**")
}

subprojects {
    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        finalizedBy(detektReportMergeSarif)
        detektReportMergeSarif.configure {
            input.from(reports.sarif.outputLocation)
        }
    }
}


