val aapLibVersion = "3.7.140"

dependencies {
    api("com.github.navikt.aap-libs:ktor-auth-azuread:$aapLibVersion")

    implementation(project(":httpklient"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.assertj:assertj-core:3.25.2")
}
