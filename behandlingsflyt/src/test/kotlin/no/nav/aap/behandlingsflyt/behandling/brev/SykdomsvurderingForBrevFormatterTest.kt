package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.Dokumentkilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykdomsvurderingForBrevFormatterTest {
    private val kilder = listOf(
        Dokumentkilde("123", "1", "Annen dokumentasjon", LocalDate.of(2026, 1, 24)),
        Dokumentkilde("123", "2", "Legeerklæring", LocalDate.of(2026, 1, 20))
    )
    private val grunnlag = SykdomsvurderingForBrev(
        BehandlingId(1), "En individuell begrunnelse.\nMed to linjer.", Bruker("Veileder"), kilder = kilder
    )

    @Test
    fun `formaterer nøyaktig én kildeliste med navn og journalføringsdato i valgorden`() {
        val forventet = """
            En individuell begrunnelse.
            Med to linjer.

            Vi har lagt vekt på følgende dokumenter i vurderingen:
            - Annen dokumentasjon (24.01.2026)
            - Legeerklæring (20.01.2026)
        """.trimIndent()

        assertThat(SykdomsvurderingForBrevFormatter.formater(grunnlag)).isEqualTo(forventet)
        assertThat(SykdomsvurderingForBrevFormatter.formater(grunnlag)).isEqualTo(forventet)
        assertThat(grunnlag.vurdering).isEqualTo("En individuell begrunnelse.\nMed to linjer.")
    }

    @Test
    fun `bevarer rå tekst uendret når ingen kilder er valgt`() {
        val medMellomrom = grunnlag.copy(vurdering = "  Opprinnelig tekst \n\n", kilder = emptyList())
        assertThat(SykdomsvurderingForBrevFormatter.formater(medMellomrom)).isEqualTo(medMellomrom.vurdering)
    }

    @Test
    fun `kilder erstatter ikke manglende eller blank begrunnelse`() {
        listOf(null, "", " \n").forEach { tekst ->
            assertThat(SykdomsvurderingForBrevFormatter.formater(grunnlag.copy(vurdering = tekst)))
                .isEqualTo(tekst)
        }
    }

    @Test
    fun `bevarer lange dokumentnavn og spesialtegn som tekst`() {
        val navn = "Vurdering av æ, ø og å & <oppfølging> " + "langt navn ".repeat(30)
        val medLangtNavn = grunnlag.copy(kilder = listOf(kilder.first().copy(dokumentnavn = navn)))

        assertThat(SykdomsvurderingForBrevFormatter.formater(medLangtNavn))
            .endsWith("- $navn (24.01.2026)")
    }
}
