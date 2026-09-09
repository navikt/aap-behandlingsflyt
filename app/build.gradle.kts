import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    alias(kelvinLibs.plugins.ktor)
    id("com.gradleup.shadow") version "9.6.1"
}

application {
    mainClass.set("no.nav.aap.behandlingsflyt.AppKt")
}

tasks {

    withType<ShadowJar> {
        // Duplikate class og ressurs-filer kan skape runtime-feil, fordi JVM-en velger den første på classpath
        // ved duplikater, og det kan være noe annet enn vår kode (og libs vi bruker) forventer.
        // Derfor logger vi en advarsel hvis vi oppdager duplikater.
        duplicatesStrategy = DuplicatesStrategy.WARN

        mergeServiceFiles()

        filesMatching(listOf("META-INF/io.netty.*", "META-INF/services/**", "META-INF/maven/**")) {
            // For disse filene fra upstream, antar vi at de er identiske hvis de har samme navn.
            // Merk at META-INF/maven/org.webjars/swagger-ui/pom.properties
            // brukes av com.papsign.ktor.openapigen.SwaggerUIVersion
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            // Vi beholder alle pom.properties fra Maven for å støtte generering av SBOM i Nais
        }

        // Helt unødvendige filer som ofte skaper duplikater
        val fjernDisseDuplikatene = listOf(
            "*.SF", "*.DSA", "*.RSA", // Signatur-filer som ikke trengs på runtime
            "*NOTICE*", "*LICENSE*", "*DEPENDENCIES*", "*README*", "*COPYRIGHT*", // til mennesker bare
            "proguard/**", // Proguard-konfigurasjoner som ikke trengs på runtime
            "com.android.tools/**" // Android build-filer som ikke trengs på runtime
        )
        fjernDisseDuplikatene.forEach { pattern -> exclude("META-INF/$pattern") }
    }
}

tasks.register<JavaExec>("runTestApp") {
    group = "application"
    description = "Kjør TestApp,."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.behandlingsflyt.TestAppKt")
}

tasks.register<JavaExec>("runTestAppMotOppgave") {
    group = "application"
    description = "Kjør TestApp mot Oppgave. Forventer at db kjører på port 5438, og oppgave-app på port 8084."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.behandlingsflyt.TestAppKt")
    environment("NAIS_CLUSTER_NAME", "LOCAL")
    environment("NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_JDBC_URL", "jdbc:postgresql://localhost:5438/postgres")
    environment("NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_USERNAME", "postgres")
    environment("NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_PASSWORD", "")
    environment("INTEGRASJON_OPPGAVESTYRING_URL", "http://localhost:8084")
}

tasks.register<JavaExec>("genererOpenApiJson") {
    group = "documentation"
    description = "Kjør generering av OpenAPI JSON-fil. Filen blir skrevet til openapi.json"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.behandlingsflyt.GenererOpenApiJsonKt")
}

tasks.register<JavaExec>("beregnCSV") {
    group = "application"
    description = "Kjør beregning basert på CSV-input fra standard input."
    classpath = sourceSets.test.get().runtimeClasspath
    standardInput = System.`in`
    mainClass.set("no.nav.aap.behandlingsflyt.BeregnMedCSVKt")
}

dependencies {
    implementation(kelvinLibs.ktor.server.status.pages)

    implementation(kelvinLibs.micrometer.prometheus)
    implementation(kelvinLibs.logback.classic)
    implementation(kelvinLibs.logstash.logback.encoder)
    implementation(kelvinLibs.jul.to.slf4j)

    implementation(libs.motor)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.motorApi)
    implementation(libs.json)
    implementation(libs.infrastructure)
    implementation(libs.server)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)

    // Auditlogging
    runtimeOnly(kelvinLibs.logback.syslog)

    implementation(project(":api"))
    implementation(project(":behandlingsflyt"))
    implementation(project(":repository"))
    implementation(kelvinLibs.hikaricp)

    implementation(kelvinLibs.opentelemetry.ktor)
    implementation(kelvinLibs.avro)
    implementation(kelvinLibs.kafka.avro.serializer)
    testImplementation(project(":lib-test"))
    implementation(libs.dbtest)
    implementation(libs.motorTestUtils)

    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(kotlin("test"))

    testImplementation(kelvinLibs.kafka.clients)

    testImplementation(kelvinLibs.testcontainers)
    testImplementation(kelvinLibs.testcontainers.postgresql)
    testImplementation(kelvinLibs.testcontainers.kafka)
}
