
plugins {
    id("behandlingsflyt.conventions")
    id("io.github.androa.gradle.plugin.avro") version "0.0.12"
}

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
}

dependencies {
    api(project(":behandlingsflyt"))
    implementation(libs.dbconnect)
    implementation(libs.infrastructure)
    implementation(libs.server)
    implementation(libs.motorApi)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(libs.avro)
    implementation(libs.kafkaAvroSerializer)
    implementation(libs.kafkaClients)
    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    compileOnly(libs.ktorHttpJvm)

    testImplementation(libs.httpklient)
    testImplementation(libs.dbtest)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.ktorServerTestHost)
    constraints {
        implementation("commons-codec:commons-codec:1.20.0")
    }
    testImplementation(libs.ktorClientContentNegotiation)
    testImplementation(libs.mockOauth2Server)
    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation(libs.mockk)
}
