val ktorVersion = "2.3.12"

dependencies {
    implementation(project(":httpklient"))
    implementation(project(":verdityper"))
    implementation(project(":dbconnect"))
    implementation(project(":sakogbehandling"))
    implementation("dev.forst:ktor-openapi-generator:0.6.1")
}