val ktorVersion = "2.3.11"

dependencies {
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.0")
}
