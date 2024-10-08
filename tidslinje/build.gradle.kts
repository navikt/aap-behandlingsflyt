val komponenterVersjon = "1.0.8"

plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon") // Periode
    implementation(project(":verdityper"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.1")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
