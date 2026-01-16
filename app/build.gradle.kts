import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
}

plugins {
    id("aap.conventions")
    alias(libs.plugins.ktor)
    id("com.gradleup.shadow") version "9.3.1"
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
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.behandlingsflyt.TestAppKt")
}

tasks.register<JavaExec>("genererOpenApiJson") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.behandlingsflyt.GenererOpenApiJsonKt")
}

tasks.register<JavaExec>("beregnCSV") {
    classpath = sourceSets.test.get().runtimeClasspath
    standardInput = System.`in`
    mainClass.set("no.nav.aap.behandlingsflyt.BeregnMedCSVKt")
}

dependencies {
    implementation(libs.ktorServerStatusPages)

    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.julToSlf4J) // trengs for postgres-logging

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
    runtimeOnly(libs.logbackSyslog)

    implementation(project(":api"))
    implementation(project(":behandlingsflyt"))
    implementation(project(":repository"))
    implementation(libs.hikariCp)
    implementation(libs.flywayDatabasePostgresql)

    runtimeOnly(libs.postgresql) // låst versjon i root build.gradle.kts

    implementation(libs.opentelemetryLogbackMdc)
    implementation(libs.opentelemetryKtor)
    implementation(libs.avro)
    implementation(libs.kafkaAvroSerializer)
    testImplementation(project(":lib-test"))
    implementation(libs.dbtest)
    implementation(libs.motorTestUtils)
    testImplementation(libs.bundles.junit)
    testImplementation(kotlin("test"))

    testImplementation(libs.kafkaClients)

    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainersPostgres)
    testImplementation(libs.testcontainersKafka)
}
