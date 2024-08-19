val ktorVersion = "2.3.12"
dependencies {
    implementation(project(":httpklient"))
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("dev.forst:ktor-openapi-generator:0.6.1")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("dev.forst:ktor-openapi-generator:0.6.1")
    
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-double-receive:$ktorVersion")
    
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation(project(":lib-test"))
}