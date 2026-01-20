// Kotlin konfigurasjonen er gitt av pluginen 'aap.conventions' i buildSrc
// og settings.gradle.kts

plugins {
    base
    `maven-publish`
    id("org.cyclonedx.bom") version "3.1.0"
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

subprojects {
    // no-op; just ensuring subprojects are configured
}

// Call the tasks of the subprojects
for (taskName in listOf<String>("clean", "build", "check")) {
    tasks.named(taskName) {
        dependsOn(subprojects.map { it.path + ":$taskName" })
    }
}