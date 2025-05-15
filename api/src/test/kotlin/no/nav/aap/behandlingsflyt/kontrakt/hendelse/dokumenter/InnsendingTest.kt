package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

// TODO, test på at lagre og hente ut LazyDokument fungerer

class InnsendingTest {

    @ParameterizedTest
    @ValueSource(
        strings = ["""{
  "student" : {
    "erStudent" : "Nei",
    "kommeTilbake" : null
  },
  "yrkesskade" : "Nei",
  "oppgitteBarn" : null
}""", """{
  "student" : {
    "erStudent" : "Avbrutt",
    "kommeTilbake" : "Ja"
  },
  "yrkesskade" : "Nei",
  "oppgitteBarn" : {
    "id" : null,
    "identer" : [ {
      "identifikator" : "12450999448",
      "aktivIdent" : true
    } ]
  }
}""", """
    {
  "student" : {
    "erStudent" : "ja",
    "kommeTilbake" : "ja"
  },
  "yrkesskade" : "ja",
  "oppgitteBarn" : {
    "identer" : [ {
      "identifikator" : "21283126223"
    } ]
  },
  "medlemskap" : {
    "harBoddINorgeSiste5År" : "ja",
    "harArbeidetINorgeSiste5År" : "ja",
    "arbeidetUtenforNorgeFørSykdom" : "ja",
    "utenlandsOpphold" : [ {
      "id" : "id",
      "land" : "NOR",
      "tilDato" : "2025-01-21",
      "fraDato" : "2025-01-24",
      "utenlandsId" : "utenlandsId",
      "iArbeid" : "nei"
    }],
    "iTtilleggArbeidUtenforNorge" : "ja"
  }
}
"""]
    )
    fun `kan deserialisere ting som allerede er i databasen`(input: String) {
        val fromJson = DefaultJsonMapper.fromJson<Melding>(input)

        assertThat(fromJson).isInstanceOf(Melding::class.java)
    }

    @Test
    fun `serialisere og deserialisere søknad-dto`() {
        val søknad = SøknadV0(
            student = SøknadStudentDto(
                erStudent = "ja", kommeTilbake = "ja"
            ),
            yrkesskade = "ja",
            oppgitteBarn = OppgitteBarn(
                identer = setOf(
                    Ident(
                        identifikator = "21283126223"
                    )
                )
            ),
            medlemskap = SøknadMedlemskapDto(
                harArbeidetINorgeSiste5År = "ja",
                harBoddINorgeSiste5År = "ja",
                arbeidetUtenforNorgeFørSykdom = "ja",
                iTilleggArbeidUtenforNorge = "ja",
                utenlandsOpphold = listOf(
                    UtenlandsPeriodeDto(
                        "AD:Andorra",
                        LocalDate.parse("2024-12-31"),
                        LocalDate.parse("2024-12-31").plusDays(3),
                        "nei",
                        "utenlandsId",
                        LocalDate.parse("2024-12-31"),
                        LocalDate.parse("2024-12-31").plusDays(3),
                    )
                )
            )
        )

        val somJSON = DefaultJsonMapper.toJson(søknad)
        assertThat(DefaultJsonMapper.fromJson<Melding>(somJSON)).isEqualTo(søknad.copy())
    }


    @Test
    fun `konkrete implementasjoner følger navnekonvensjon`() {
        val alleKonkreteMeldingsTyper = Melding::class.sealedSubclasses.flatMap { outer -> outer.sealedSubclasses }

        // Alle implementasjoner har klassenavn med versjon "V0", "V1", etc
        assertThat(alleKonkreteMeldingsTyper).allSatisfy {
            assertThat(it.simpleName).matches("[A-Za-zÆØÅæøå]+V\\d")
        }
    }

    @Test
    fun `serialisere og deserialisere aktivitetskort`() {
        val aktivitetskort = AktivitetskortV0(
            fraOgMed = LocalDate.of(2023, 1, 1),
            tilOgMed = LocalDate.of(2023, 1, 31),
        )

        val somJSON = DefaultJsonMapper.toJson(aktivitetskort)
        assertThat(DefaultJsonMapper.fromJson<Melding>(somJSON)).isEqualTo(aktivitetskort)
    }

    @Test
    fun `serialisere og deserialisere meldekort`() {
        val aktivitetskort = MeldekortV0(
            harDuArbeidet = true,
            timerArbeidPerPeriode = listOf(
                ArbeidIPeriodeV0(
                    fraOgMedDato = LocalDate.of(2023, 1, 1),
                    tilOgMedDato = LocalDate.of(2023, 1, 31),
                    timerArbeid = 25.0,
                )
            )
        )

        val somJSON = DefaultJsonMapper.toJson(aktivitetskort)
        assertThat(DefaultJsonMapper.fromJson<Melding>(somJSON)).isEqualTo(aktivitetskort)
    }

    @Test
    fun `serialisere og deserialisere klage`() {
        val klage = KlageV0(
            kravMottatt = LocalDate.of(2023, 1, 1)
        )

        val somJSON = DefaultJsonMapper.toJson(klage)
        assertThat(DefaultJsonMapper.fromJson<Melding>(somJSON)).isEqualTo(klage)
    }

    @Test
    fun `kan deserialisere eksempel-json`() {
        val eksempelJSON = """
            {
    "meldingType" : "SøknadV0",
    "student" : {
      "erStudent" : "ja",
      "kommeTilbake" : "ja"
    },
    "yrkesskade" : "ja",
    "oppgitteBarn" : null,
    "medlemskap": null,
    "nyttFelt" : 2
  }
        """.trimIndent()

        val melding = assertDoesNotThrow { DefaultJsonMapper.fromJson<Melding>(eksempelJSON) }
        assertThat(melding).isInstanceOf(SøknadV0::class.java)
    }
}