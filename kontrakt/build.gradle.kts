import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
    id("aap.publishing")
    `java-library`
}

dependencies {
    api(libs.jacksonAnnotations)
    api(libs.ktorOpenApiGenerator)
    implementation(libs.json)
    compileOnly(libs.tilgangKontrakt)

    testRuntimeOnly(libs.tilgangKontrakt)
}

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
}
