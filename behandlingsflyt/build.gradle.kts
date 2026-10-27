plugins {
    id("aap.conventions")
}

tasks {
    val projectProps = register<WriteProperties>("projectProps") {
        destinationFile = layout.buildDirectory.file("behandlingsflyt-version.properties")
        property("project.version", "development")
    }

    processResources {
        // Depend on output of the task to create properties,
        // so the properties file will be part of the Java resources.

        from(projectProps)
    }
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("behandlingsflyt.md")
    }
}

dependencies {
    api(project(":kontrakt"))
    implementation(kelvinLibs.micrometer.prometheus)
    implementation(kelvinLibs.logback.classic)
    implementation(kelvinLibs.logstash.logback.encoder)
    implementation(kelvinLibs.opentelemetry.annotations)

    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    api(libs.brevKontrakt)
    api(libs.meldekortKontrakt)
    api(libs.motor)
    api(libs.gateway)
    api(libs.utbetalKontrakt)
    api(libs.dokumentinnhentingKontrakt)
    api(libs.arenaoppslag.kontrakt)
    implementation(libs.dbconnect)
    // TODO: fjern denne avhengigheten når alle RestClient-instanser er i repository-modulen
    implementation(libs.httpklient)
    implementation(libs.json)
    implementation(libs.infrastructure)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(kelvinLibs.kafka.clients)
    implementation(kotlin("reflect"))

    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation(libs.dbtest)
    testImplementation(kelvinLibs.tabletest)
    implementation(libs.motorTestUtils)
    testImplementation(kelvinLibs.bundles.junit)

    testImplementation(kelvinLibs.testcontainers.postgresql)
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(kelvinLibs.mockk)
    testImplementation(kotlin("test"))
}
