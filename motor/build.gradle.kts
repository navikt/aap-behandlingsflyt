dependencies {
    implementation(project(":verdityper"))
    implementation(project(":dbflyway"))
    implementation(project(":dbconnect"))
    implementation("org.slf4j:slf4j-api:2.0.13")

    testImplementation(project(":dbtestdata"))
    testImplementation(project(":dbtest"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation(kotlin("test"))
}