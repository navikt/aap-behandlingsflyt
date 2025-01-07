import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("behandlingsflyt.conventions")
    `maven-publish`
    `java-library`
}

val tilgangVersjon = "0.0.81"
val junitVersion = "5.11.3"

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.18.1")
    api("no.nav:ktor-openapi-generator:1.0.75")
    implementation("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.27.2")

}

group = "no.nav.aap.behandlingsflyt"

apply(plugin = "maven-publish")
apply(plugin = "java-library")

kotlin {
    explicitApi = ExplicitApiMode.Warning
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name
            version = project.findProperty("version")?.toString() ?: "0.0.0"
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/aap-behandlingsflyt")
            credentials {
                username = "x-access-token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}