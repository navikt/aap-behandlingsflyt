package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class KravVurderingLøsningDtoTest {

    @Test
    fun `Deserialiser nytt krav`() {
        val forventet = NyttKravLøsningDto(
            referanse = UUID.fromString("89cbb68f-f7f7-459a-909d-25edec9971ce"),
            journalpostId = JournalpostId("123456789"),
            begrunnelse = "Begrunnelse",
            søknadsdato = Søknadsdato(dato = 1 februar 2026, SøknadsdatoÅrsak.SøknadMottatt),
            overstyrMuligRettFra = OverstyrMuligRettFra(
                dato = 1 januar 2026,
                OverstyrMuligRettFraÅrsak.MisvisendeOpplysninger
            )
        )

        val løsning = """
            {
              "kravType" : "NYTT_KRAV_AAP",
              "referanse" : "89cbb68f-f7f7-459a-909d-25edec9971ce",
              "journalpostId" : {
                "identifikator" : "123456789"
              },
              "begrunnelse" : "Begrunnelse",
              "søknadsdato" : {
                "dato" : "2026-02-01",
                "årsak" : "SøknadMottatt"
              },
              "overstyrMuligRettFra" : {
                "dato" : "2026-01-01",
                "årsak" : "MisvisendeOpplysninger"
              }
            }
        """.trimIndent()

        val faktisk = DefaultJsonMapper.fromJson(løsning, KravVurderingLøsningDto::class.java)
        assertThat(faktisk).isEqualTo(forventet)

        val vurdering = faktisk.tilVurdering(BehandlingId(1), Bruker("En ident"), Instant.now())
        assertThat(vurdering).isInstanceOf(NyttKrav::class.java)
    }

    @Test
    fun `Deserialiser gjenopptak`() {
        val forventet = GjenopptakKravLøsningDto(
            referanse = null,
            journalpostId = JournalpostId("123456789"),
            begrunnelse = "Begrunnelse",
            søknadsdato = Søknadsdato(dato = 31 desember 2025, SøknadsdatoÅrsak.BrukerHarSøktTidligere),
            overstyrMuligRettFra = null
        )

        val løsning = """
            {
              "kravType" : "GJENOPPTAK",
              "journalpostId" : {
                "identifikator" : "123456789"
              },
              "begrunnelse" : "Begrunnelse",
              "søknadsdato" : {
                "dato" : "2025-12-31",
                "årsak" : "BrukerHarSøktTidligere"
              }
            }
        """.trimIndent()

        val faktisk = DefaultJsonMapper.fromJson(løsning, KravVurderingLøsningDto::class.java)
        assertThat(faktisk).usingRecursiveComparison().ignoringFields("referanse").isEqualTo(forventet)
        assertThat((faktisk as GjenopptakKravLøsningDto).referanse).isNotNull()

        val vurdering = { faktisk.tilVurdering(BehandlingId(1), Bruker("En ident"), Instant.now()) }
        assertThrows<UgyldigForespørselException> { vurdering() }
    }

    @Test
    fun `Deserialiser tilleggsopplysning`() {
        val forventet = TilleggsopplysningKravLøsningDto(
            referanse = UUID.fromString("89cbb68f-f7f7-459a-909d-25edec9971ce"),
            journalpostId = JournalpostId("123456789"),
            begrunnelse = "Begrunnelse",
        )

        val løsning = """
            {
              "kravType" : "TILLEGGSOPPLYSNING",
              "referanse" : "89cbb68f-f7f7-459a-909d-25edec9971ce",
              "begrunnelse" : "Begrunnelse",
              "journalpostId" : {
                "identifikator" : "123456789"
              }
            }
        """.trimIndent()

        val faktisk = DefaultJsonMapper.fromJson(løsning, KravVurderingLøsningDto::class.java)
        assertThat(faktisk).isEqualTo(forventet)

        val vurdering = faktisk.tilVurdering(BehandlingId(1), Bruker("En ident"), Instant.now())
        assertThat(vurdering).isInstanceOf(Tilleggsopplysning::class.java)
    }

    @Test
    fun `Deserialiser klage`() {
        val forventet = KlageKravLøsningDto(
            referanse = null,
            journalpostId = JournalpostId("123456789"),
            begrunnelse = "Begrunnelse",
        )

        val løsning = """
            {
              "kravType" : "KLAGE",
              "begrunnelse" : "Begrunnelse",
              "journalpostId" : {
                "identifikator" : "123456789"
              }
            }
        """.trimIndent()

        val faktisk = DefaultJsonMapper.fromJson(løsning, KravVurderingLøsningDto::class.java)
        assertThat(faktisk).isEqualTo(forventet)

        val vurdering = { faktisk.tilVurdering(BehandlingId(1), Bruker("En ident"), Instant.now()) }
        assertThrows<UgyldigForespørselException> { vurdering() }
    }
}