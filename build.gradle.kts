// Kotlin konfigurasjonen er gitt av pluginen 'aap.conventions' i buildSrc
// og settings.gradle.kts

plugins {
    base
    `maven-publish`
    id("org.cyclonedx.bom") version "3.2.0"
    id("aap.conventions")
}

// Produser en SBOM (Software Bill of Materials) og last den opp som et Maven-artifact
group = "no.nav.aap.behandlingsflyt"
version = project.findProperty("version")?.toString() ?: "0.0.0"

tasks {
    withType<PublishToMavenRepository>().configureEach {
        dependsOn(cyclonedxBom)
    }

    cyclonedxBom {
        jsonOutput.unsetConvention() // ikke lag både json og xml
    }
}

// cyclonedxDirectBom-tasker resolver classpaths på tvers av subprojects, men
// deklarerer ikke task-avhengigheter til kompileringstaskene som produserer disse klassene.
val allCompileTasks = subprojects.flatMap { sub ->
    sub.tasks.matching { it.name == "compileKotlin" || it.name == "compileJava" }
}
subprojects.forEach { sub ->
    sub.tasks.matching { it.name == "cyclonedxDirectBom" }.configureEach {
        dependsOn(allCompileTasks)
    }
}

publishing {
    publications {
        create<MavenPublication>("bomXml") {
            val sbomFile = tasks.cyclonedxBom.get().xmlOutput.get()
            artifact(sbomFile) {
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
for (taskName in listOf<String>("clean", "build", "check")) {
    tasks.named(taskName) {
        dependsOn(subprojects.map { it.tasks.named(taskName) })
    }
}

// Merge Detekt reports from all subprojects
val detektReportMergeSarif by tasks.registering(dev.detekt.gradle.report.ReportMergeTask::class) {
    output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

subprojects {
    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        finalizedBy(detektReportMergeSarif)
        detektReportMergeSarif.configure {
            input.from(reports.sarif.outputLocation)
        }
    }
}
