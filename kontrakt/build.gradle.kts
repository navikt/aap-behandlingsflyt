import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("behandlingsflyt.conventions")
    `maven-publish`
    `java-library`
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.19.2")
    api("no.nav:ktor-openapi-generator:1.0.121")
    compileOnly(libs.tilgangKontrakt)


    testRuntimeOnly(libs.tilgangKontrakt)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.json)
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
