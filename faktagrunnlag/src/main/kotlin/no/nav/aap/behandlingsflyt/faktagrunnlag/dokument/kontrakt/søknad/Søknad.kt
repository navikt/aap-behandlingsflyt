package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad

class Søknad(
    val student: SøknadStudentDto,
    val yrkesskade: String
) {
    fun harYrkesskade(): Boolean {
        return yrkesskade.uppercase() == "JA"
    }
}

class SøknadStudentDto(
    val erStudent: String
) {
    fun erStudent() = erStudent.uppercase() == "JA"
}
