package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// TODO, test på at lagre og hente ut LazyDokument fungerer

class MeldingTest {
    @Test
    fun `fsdf sdf `() {
//        val subtypes = Melding::class.sealedSubclasses.map { it.java }.toList()
//        DefaultJsonMapper.objectMapper().registerSubtypes(subtypes)
//        println(subtypes)

        val x = SøknadV0(
            student = SøknadStudentDto(
                erStudent = "ja",
                kommeTilbake = "ja"
            ),
            yrkesskade = "ja",
            oppgitteBarn = OppgitteBarn(
                identer = setOf("334")
            ),
//            endaEtFelt = "xxx"
        )

        val somJSON = DefaultJsonMapper.toJson(x)
//        assertThat(somJSON).isEqualTo(1212)

        assertThat(DefaultJsonMapper.fromJson<Melding>(somJSON)).isEqualTo(x.copy())
    }

    @Test
    fun `sdfsdf `() {
//        val subtypes = Melding::class.sealedSubclasses.map { it.java }.toList()
//        DefaultJsonMapper.objectMapper().registerSubtypes(subtypes)
//        println(subtypes)


        val s = """
            {
    "meldingType" : "SøknadV0",
    "student" : {
      "erStudent" : "ja",
      "kommeTilbake" : "ja"
    },
    "yrkesskade" : "ja",
    "oppgitteBarn" : {
      "identer" : [ "334" ]
    },
    "nyttFelt" : 2
  }
        """.trimIndent()

        DefaultJsonMapper.fromJson<Melding>(s)
    }

    @Test
    fun `sddasd fsdf `() {
        val s = """
{
  "student" : {
    "erStudent" : "NEI",
    "kommeTilbake" : "IKKE_OPPGITT"
  },
  "yrkesskade" : "NEI",
  "oppgitteBarn" : null
}
        """.trimIndent()

        val fromJson = DefaultJsonMapper.fromJson<Melding>(s)

        assertThat(fromJson).isInstanceOf(SøknadV0::class.java)
    }
}