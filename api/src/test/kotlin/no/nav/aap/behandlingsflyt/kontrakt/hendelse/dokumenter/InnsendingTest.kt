package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalDateTime


class InnsendingTest {

    @Test
    fun `uførehendelse krever en uførevedtaksmelding`() {
        val meldinger = listOf(null, KorrigerSøknadsdatoV0("Feil meldingstype"))

        for (melding in meldinger) {
            assertThatThrownBy {
                Innsending(
                    saksnummer = Saksnummer("TEST_1"),
                    referanse = InnsendingReferanse(InnsendingReferanse.Type.UFØREVEDTAK_HENDELSE_ID, "test"),
                    type = InnsendingType.UFØRE_VEDTAK_HENDELSE,
                    mottattTidspunkt = LocalDateTime.of(2026, 10, 10, 12, 0),
                    melding = melding
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `uførehendelse godtar begge versjoner av uførevedtak`() {
        val dato = LocalDate.of(2026, 10, 10)
        val meldinger = listOf(
            UførevedtakV0("12345678901", dato, UførevedtakResultat.INNV, false, UførevedtakKilde.PENSJON),
            UførevedtakV1("12345678901", dato, UførevedtakResultat.INNV, false, UførevedtakKilde.PENSJON, dato)
        )

        for (melding in meldinger) {
            val innsending = Innsending(
                saksnummer = Saksnummer("TEST_1"),
                referanse = InnsendingReferanse(InnsendingReferanse.Type.UFØREVEDTAK_HENDELSE_ID, "test"),
                type = InnsendingType.UFØRE_VEDTAK_HENDELSE,
                mottattTidspunkt = dato.atStartOfDay(),
                melding = melding
            )
            assertThat(innsending.melding).isEqualTo(melding)
        }
    }

    @Test
    fun `deserialisering - komplett Søknad`() {
        @Language("JSON")
        val input = """
               {
          "student" : {
            "erStudent" : "Ja",
            "kommeTilbake" : "Vet ikke"
          },
          "yrkesskade" : "ja",
          "oppgitteBarn" : {
            "identer" : [ {
              "identifikator" : "21283126223"
            } ],
            "barn": [
                {
                    "navn" : "Rask Trombone",
                    "fødselsdato" : "2020-01-01",
                    "ident" : { "identifikator": "11223312345" },
                    "relasjon" : "FOSTERFORELDER"
                }
            ]
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
          },
          "andreUtbetalinger": {
            "lønn": "Ja",
            "afp": {
              "hvemBetaler": "arbeidsgiver"
            },
            "stønad": [ "OMSORGSSTØNAD", "AFP" ]
          }
        }
        """

        val fromJson = DefaultJsonMapper.fromJson<Melding>(input)

        assertThat(fromJson).isInstanceOf(Melding::class.java)
    }

    @Test
    fun `deserialsering - oppgitteBarn med kun identer`() {
        val json = """
        {
          "student" : {
            "erStudent" : "Avbrutt",
            "kommeTilbake" : "Vet ikke"
          },
          "yrkesskade" : "Nei",
          "oppgitteBarn" : {
            "id" : null,
            "identer" : [ {
              "identifikator" : "12450999448",
              "aktivIdent" : true
            } ]
          }
        }
        """

        val fromJson = DefaultJsonMapper.fromJson<Melding>(json)

        assertThat(fromJson).isInstanceOf(Melding::class.java)
        fromJson as SøknadV0
        assertThat(fromJson.student?.kommeTilbake).isEqualTo(JaNeiVetIkke.VetIkke)
    }

    @Test
    fun `kan deserialisere ting som allerede er i databasen`() {
        val json = """
        {
          "student" : {
            "erStudent" : "Nei",
            "kommeTilbake" : null
          },
          "yrkesskade" : "Nei",
          "oppgitteBarn" : null
        }
        """

        val fromJson = DefaultJsonMapper.fromJson<Melding>(json)

        assertThat(fromJson).isInstanceOf(Melding::class.java)
    }

    @Test
    fun `serialisere og deserialisere søknad-dto`() {
        val søknad = SøknadV0(
            student = SøknadStudentDto(
                erStudent = StudentStatus.Ja, kommeTilbake = JaNeiVetIkke.VetIkke
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
        // Skal serialisere til "Vet ikke"
        assertThat(somJSON).contains("Vet ikke")
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
      "erStudent" : "Ja",
      "kommeTilbake" : "Ja"
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

    @Test
    fun `parse oppfølgingsoppgave fra full melding`() {
        @Language("JSON")
        val s = """{
  "saksnummer": "4LDQPDS",
  "referanse": {
    "type": "SAKSBEHANDLER_KELVIN_REFERANSE",
    "verdi": "e8a9ca4b-83b9-44dc-b54a-df2199a4a244"
  },
  "type": "OPPFØLGINGSOPPGAVE",
  "kanal": "DIGITAL",
  "mottattTidspunkt": "2025-07-16T10:15:53.359Z",
  "melding": {
    "meldingType": "OppfølgingsoppgaveV0",
    "datoForOppfølging": "2025-07-16",
    "hvaSkalFølgesOpp": "dsfsdf",
    "reserverTilBruker": "dd",
    "hvemSkalFølgeOpp": "NasjonalEnhet"
  }
}
"""
        val obj = DefaultJsonMapper.fromJson<Innsending>(s)

        assertThat(obj.melding).isInstanceOf(OppfølgingsoppgaveV0::class.java)
        val oppfølgingsOppgaveActual = obj.melding as OppfølgingsoppgaveV0
        assertThat(oppfølgingsOppgaveActual.hvemSkalFølgeOpp).isEqualTo(HvemSkalFølgeOpp.NasjonalEnhet)
    }


    @ParameterizedTest
    @MethodSource("hvemSkalFølgeOppMethodSource")
    fun `serialisering av oppfølgingsoppgavejson`(hvemSkalFølgeOpp: HvemSkalFølgeOpp) {
        val oppfølgingsoppgave = OppfølgingsoppgaveV0(
            datoForOppfølging = LocalDate.now(),
            hvemSkalFølgeOpp = hvemSkalFølgeOpp,
            reserverTilBruker = "xx",
            hvaSkalFølgesOpp = "da",
            opprettetAv = null
        )

        val json = DefaultJsonMapper.toJson(oppfølgingsoppgave)

        val tilbakeIgjen = DefaultJsonMapper.fromJson<Oppfølgingsoppgave>(json)

        assertThat(oppfølgingsoppgave).isEqualTo(tilbakeIgjen)
    }

    @Test
    fun `parse korriger søknadsdato fra full melding`() {
        @Language("JSON")
        val s = """{
  "saksnummer": "4LDQPDS",
  "referanse": {
    "type": "MANUELL_OPPRETTELSE",
    "verdi": "e8a9ca4b-83b9-44dc-b54a-df2199a4a244"
  },
  "type": "KORRIGER_SØKNADSDATO",
  "kanal": "DIGITAL",
  "mottattTidspunkt": "2025-07-16T10:15:53.359Z",
  "melding": {
    "meldingType": "KorrigerSøknadsdatoV0",
    "begrunnelse": "Feilregistrert søknadsdato"
  }
}
"""
        val obj = DefaultJsonMapper.fromJson<Innsending>(s)

        assertThat(obj.melding).isInstanceOf(KorrigerSøknadsdatoV0::class.java)
        val korrigerSøknadsdatoActual = obj.melding as KorrigerSøknadsdatoV0
        assertThat(korrigerSøknadsdatoActual.begrunnelse).isEqualTo("Feilregistrert søknadsdato")
    }

    @Test
    fun `serialisere og deserialisere korriger søknadsdato`() {
        val korrigerSøknadsdato = KorrigerSøknadsdatoV0(
            begrunnelse = "Feilregistrert søknadsdato"
        )

        val somJSON = DefaultJsonMapper.toJson(korrigerSøknadsdato)
        assertThat(DefaultJsonMapper.fromJson<Melding>(somJSON)).isEqualTo(korrigerSøknadsdato)
    }

    companion object {
        @JvmStatic
        fun hvemSkalFølgeOppMethodSource() = listOf(
            HvemSkalFølgeOpp.NasjonalEnhet,
            HvemSkalFølgeOpp.Lokalkontor
        )
    }
}
