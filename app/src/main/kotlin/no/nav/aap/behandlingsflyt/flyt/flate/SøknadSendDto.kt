package no.nav.aap.behandlingsflyt.flyt.flate

class SøknadSendDto(
    val saksnummer: String,
    val journalpostId: String,
    val søknad: SøknadDto
)

class SøknadDto(
    val student: SøknadStudentDto
)

class SøknadStudentDto(
    val erStudent: String
) {
    fun erStudent() = erStudent == "JA"
}


/*

{ "saksnummer":"4LDRRYo",
  "journalpostId":"453865487",
  "søknad":{
      "barn":[],
      "sykepenger":"Nei",
      "medlemskap":{
        "harBoddINorgeSiste5År":"Ja",
        "arbeidetUtenforNorgeFørSykdom":"Nei"
      },
      "yrkesskade":"Nei",
      "student":{
        "erStudent":"Ja"
      },
      "andreUtbetalinger":{
        "lønn":"Nei","stønad":["NEI"],
        "afp":{}
      },
      "søknadBekreft":true,
      "version":1,
      "etterspurtDokumentasjon":[]
  }
}
 */