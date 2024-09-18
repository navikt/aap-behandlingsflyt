val komponenterVersjon = "0.0.59"

dependencies {
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    implementation(project(":verdityper"))
}
