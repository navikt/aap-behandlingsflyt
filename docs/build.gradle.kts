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
    val outputFile = layout.buildDirectory.file("dokka/html/avklaringsbehov.html")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("no.nav.aap.docs.DefinisjonHtmlGeneratorKt")
    args(outputFile.get().asFile.absolutePath)
    outputs.file(outputFile)
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