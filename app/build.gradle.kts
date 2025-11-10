import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val opentelemetryVersion = "2.21.0-alpha"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
}

plugins {
    id("behandlingsflyt.conventions")
    alias(libs.plugins.ktor)
    id("com.gradleup.shadow") version "9.2.2"
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
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerStatusPages)

    implementation("io.micrometer:micrometer-registry-prometheus:1.16.0")
    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("org.slf4j:jul-to-slf4j:2.0.17") // trengs for postgres-logging

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
    runtimeOnly("com.papertrailapp:logback-syslog4j:1.0.0")

    implementation(project(":api"))
    implementation(project(":behandlingsflyt"))
    implementation(project(":repository"))
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.16.0")

    runtimeOnly("org.postgresql:postgresql") // låst versjon i root build.gradle.kts

    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:${opentelemetryVersion}")
    implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-3.0:${opentelemetryVersion}")
    implementation("org.apache.avro:avro:1.12.1")
    implementation("io.confluent:kafka-avro-serializer:7.6.0")
    testImplementation(project(":lib-test"))
    implementation(libs.dbtest)
    implementation(libs.motorTestUtils)
    testImplementation(libs.bundles.junit)
    testImplementation("org.testcontainers:postgresql:1.21.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(kotlin("test"))
    testImplementation("org.testcontainers:kafka:1.21.3")
    testImplementation("org.apache.kafka:kafka-clients:4.1.0")
}
