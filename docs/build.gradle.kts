plugins {
    kotlin("jvm") apply false
    id("aap.conventions")
}

dependencies {
    rootProject.subprojects.forEach { subproject ->
        dokka(project(":" + subproject.name))
    }
    implementation(project(":kontrakt"))
    implementation(libs.tilgangKontrakt)
}

val generateAvklaringsbehovHtml =  tasks.register<JavaExec>("generateAvklaringsbehovHtml") {
    description = "Generer HTML-fil med tabell over avklaringsbehov."
    val outputDirectory = layout.buildDirectory.dir("dokka/html")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("no.nav.aap.docs.DefinisjonHtmlGeneratorKt")
    args(outputDirectory.get().asFile.absolutePath)
    outputs.file(outputDirectory.map { it.file("avklaringsbehov.html") })
}

tasks.named("dokkaGenerate") {
    finalizedBy(generateAvklaringsbehovHtml)
}

dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            remoteUrl("https://github.com/navikt/aap-behandlingsflyt/blob/main")
            localDirectory.set(rootDir)
        }
    }
    dokkaPublications {
        configureEach {
            suppressObviousFunctions.set(true)
            suppressInheritedMembers.set(false)
            includes.from("DocsModule.md")
        }
    }
}