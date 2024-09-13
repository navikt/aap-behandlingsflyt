val komponenterVersjon = "0.0.51"

dependencies {
    implementation(project(":dbtest"))
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    implementation(project(":verdityper"))
}
