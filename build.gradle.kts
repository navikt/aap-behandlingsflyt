// top-level build gradle

plugins {
    base
    `maven-publish`
    id("org.cyclonedx.bom") version "3.1.0"
}

allprojects {
    configurations.configureEach {
        resolutionStrategy {
            // Lås postgres-versjonen til en "known good" versjon for nå
            force("org.postgresql:postgresql:42.7.8")
        }
    }
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
}