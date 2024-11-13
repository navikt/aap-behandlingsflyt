val komponenterVersjon = "1.0.58"

plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon") // Periode
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation(project(":verdityper"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
