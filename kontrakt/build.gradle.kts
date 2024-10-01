plugins {
    id("behandlingsflyt.conventions")
    `maven-publish`
    `java-library`
}
val komponenterVersjon = "0.0.86"

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.18.0")
    api("no.nav:ktor-openapi-generator:1.0.34")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.1")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")

}

group = "no.nav.aap.behandlingsflyt"

apply(plugin = "maven-publish")
apply(plugin = "java-library")

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