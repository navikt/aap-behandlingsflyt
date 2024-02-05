dependencies {
    implementation(project(":sakogbehandling"))
    implementation(project(":verdityper"))
    implementation(project(":tidslinje"))
    implementation(project(":dbflyway"))
    implementation(project(":dbconnect"))

    implementation("dev.forst:ktor-openapi-generator:0.6.1")
    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")

    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.assertj:assertj-core:3.25.2")

    testImplementation(project(":dbtestdata"))
    testImplementation(project(":dbtest"))
}