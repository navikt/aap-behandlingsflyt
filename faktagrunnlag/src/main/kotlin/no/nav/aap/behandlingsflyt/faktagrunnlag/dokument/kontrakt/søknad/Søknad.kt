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
    val erStudent: String,
    val kommeTilbake: String? = null
) {
    fun erStudent(): String {
      if (erStudent.uppercase() == "JA"){
          return "JA"
      } else if (erStudent.uppercase() === "AVBRUTT"){
          return "AVBRUTT"
      } else {
          return "NEI"
      }
    }

    fun skalGjennopptaStudie(): String{
        if (kommeTilbake?.uppercase() == "JA"){
            return "JA"
        } else if (kommeTilbake?.uppercase() == "NEI"){
            return "NEI"
        } else if (kommeTilbake?.uppercase() == "VET IKKE"){
            return "VET_IKKE"
        } else{
            return "IKKE_SPESIFISERT"
        }
    }
}
