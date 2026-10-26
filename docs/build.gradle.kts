import org.gradle.process.CommandLineArgumentProvider

plugins {
    id("aap.conventions")
}

dependencies {
    rootProject.subprojects.forEach { subproject ->
        dokka(project(":" + subproject.name))
    }
    implementation(project(":kontrakt"))
    implementation(project(":behandlingsflyt"))
    implementation(libs.tilgangKontrakt)
}

val generateAvklaringsbehovHtml =  tasks.register<JavaExec>("generateAvklaringsbehovHtml") {
    group = "documentation"
    description = "Generer HTML-fil med tabell over avklaringsbehov."
    val outputDirectory = layout.buildDirectory.dir("dokka/html")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("no.nav.aap.docs.DefinisjonHtmlGeneratorKt")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(outputDirectory.get().asFile.absolutePath)
    })
    outputs.file(outputDirectory.map { it.file("avklaringsbehov.html") })
}

tasks.named("dokkaGenerate") {
    finalizedBy(generateAvklaringsbehovHtml)
}

dokka {
    pluginsConfiguration.html {
        customAssets.from("assets/diff-highlighting.js")
        customStyleSheets.from("assets/diff-highlighting.css")
    }
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
            includes.from("DocsModule.md", "playbook.md")
        }
    }
}