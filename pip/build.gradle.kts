val ktorVersion = "2.3.12"

dependencies {
    implementation(project(":httpklient"))
    implementation(project(":verdityper"))
    implementation(project(":dbconnect"))
    implementation(project(":sakogbehandling"))
    implementation(project(":faktagrunnlag"))
    implementation("dev.forst:ktor-openapi-generator:0.6.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation(project(":lib-test"))
}