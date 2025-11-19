// top-level build gradle

plugins {
    base
    id("behandlingsflyt.conventions")
}

allprojects {
    configurations.configureEach {
        resolutionStrategy {
            // Lås postgres-versjonen til en "known good" versjon for nå
            force("org.postgresql:postgresql:42.7.8")
        }
    }
}

dependencies {
    rootProject.subprojects.forEach { subproject ->
        dokka(project(":" + subproject.name))
    }
}