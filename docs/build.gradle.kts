plugins {
    kotlin("jvm") apply false
    id("behandlingsflyt.conventions")
}

dependencies {
    rootProject.subprojects.forEach { subproject ->
        dokka(project(":" + subproject.name))
    }
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