dependencies {
    implementation(project(":verdityper"))
    implementation(project(":dbflyway"))
    implementation(project(":dbconnect"))
    implementation("org.slf4j:slf4j-api:2.0.14")
    implementation("dev.forst:ktor-openapi-generator:0.6.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")

    testImplementation(project(":dbtestdata"))
    testImplementation(project(":dbtest"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation(kotlin("test"))
}