package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.Dokumentkilde
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykdomsvurderingForBrevDtoTest {
    @Test
    fun `serialiserer kildekontrakten for gjeldende og historiske vurderinger`() {
        val kilde = Dokumentkilde("123", "456", "Legeerklæring", LocalDate.of(2026, 1, 20))
        val vurdering = SykdomsvurderingForBrevVurderingDto(
            vurdering = "Individuell begrunnelse",
            vurderingerMeta = VurderingerMetaResponse(),
            kilder = listOf(kilde, kilde.copy(dokumentInfoId = "789", dokumentnavn = "Vedlegg")),
        )
        val historisk = vurdering.copy(kilder = listOf(kilde.copy(dokumentnavn = "Opprinnelig navn")))
        val response = SykdomsvurderingForBrevDto(vurdering, listOf(historisk), kanSaksbehandle = false)

        val deserialisert = DefaultJsonMapper.fromJson<SykdomsvurderingForBrevDto>(DefaultJsonMapper.toJson(response))
        val forventedeKilder = DefaultJsonMapper.fromJson<List<Dokumentkilde>>(
            """
                [{
                  "journalpostId": "123",
                  "dokumentInfoId": "456",
                  "dokumentnavn": "Legeerklæring",
                  "journalføringsdato": "2026-01-20"
                }, {
                  "journalpostId": "123",
                  "dokumentInfoId": "789",
                  "dokumentnavn": "Vedlegg",
                  "journalføringsdato": "2026-01-20"
                }]
            """.trimIndent()
        )
        assertThat(deserialisert.vurdering?.kilder).containsExactlyElementsOf(forventedeKilder)
        assertThat(deserialisert.historiskeVurderinger).containsExactly(historisk)
        assertThat(deserialisert).isEqualTo(response)
    }

    @Test
    fun `serialiserer eldre vurderinger med tom kildeliste`() {
        val vurdering = SykdomsvurderingForBrevVurderingDto("Begrunnelse", VurderingerMetaResponse())
        val deserialisert = DefaultJsonMapper.fromJson<SykdomsvurderingForBrevVurderingDto>(
            DefaultJsonMapper.toJson(vurdering)
        )

        assertThat(deserialisert.kilder).isEmpty()
        assertThat(deserialisert).isEqualTo(vurdering)
    }
}
