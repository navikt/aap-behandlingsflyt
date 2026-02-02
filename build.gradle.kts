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

// Call the tasks of the subprojects
for (taskName in listOf<String>("clean", "build", "check")) {
    tasks.named(taskName) {
        dependsOn(subprojects.map { it.path + ":$taskName" })
    }
}

// Emit a JSON-formatted list of check tasks to be run in CI
tasks.register("testMatrix") {
    notCompatibleWithConfigurationCache("Accesses project at execution time")
    doLast {
        val checkTaskPaths = subprojects
            .filter { it.name != "docs" }
            .mapNotNull { it.tasks.findByName("check")?.path }
        val json = checkTaskPaths.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" }
        println(json)
    }
}

// If we're executing the `testMatrix` task, disable tests and other slow tasks
// so that we can get a result quickly.
gradle.taskGraph.whenReady {
    if (hasTask(tasks.named("testMatrix").get())) {
        subprojects.forEach { subproject ->
            subproject.tasks.withType<Test>().configureEach {
                enabled = false
            }
            subproject.tasks.findByName("shadowJar")?.enabled = false
            subproject.tasks.findByName("javadoc")?.enabled = false
        }
    }
}
