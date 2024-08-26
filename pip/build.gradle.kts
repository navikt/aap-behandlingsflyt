val ktorVersion = "2.3.12"

dependencies {
    implementation(project(":httpklient"))
    implementation(project(":verdityper"))
    implementation(project(":dbconnect"))
    implementation(project(":sakogbehandling"))
    implementation(project(":faktagrunnlag"))
    implementation(project(":tilgang"))
    implementation("no.nav:ktor-openapi-generator:1.0.6")
    implementation("io.ktor:ktor-http-jvm:$ktorVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation(project(":lib-test"))
}