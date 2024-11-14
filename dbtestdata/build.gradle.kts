val komponenterVersjon = "1.0.59"

plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    implementation(project(":verdityper"))
}
