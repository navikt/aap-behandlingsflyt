package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KorrigerSøknadsdatoV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.HvemSkalFølgeOpp
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MottattHendelseUtlederTest {

    @Test
    fun `opprettetAv på innsending prioriteres fremfor legacy opprettetAv uten å endre meldingen`() {
        val melding = ManuellRevurderingV0(emptyList(), "Revurdering", opprettetAv = "legacy")

        assertThat(MottattHendelseUtleder.utledOpprettetAv(melding, opprettetAv = "ny"))
            .isEqualTo(Bruker("ny"))
        assertThat(melding.opprettetAv).isEqualTo("legacy")
    }

    @Test
    fun `manuell revurdering bruker legacy opprettetAv når innsending mangler opprettetAv`() {
        val melding = ManuellRevurderingV0(emptyList(), "Revurdering", opprettetAv = "legacy")

        assertThat(MottattHendelseUtleder.utledOpprettetAv(melding, opprettetAv = null))
            .isEqualTo(Bruker("legacy"))
    }

    @Test
    fun `manglende opprettetAv forblir ukjent`() {
        assertThat(MottattHendelseUtleder.utledOpprettetAv(null)).isNull()
        assertThat(MottattHendelseUtleder.utledOpprettetAv(ManuellRevurderingV0(emptyList(), "Revurdering")))
            .isNull()
        assertThat(MottattHendelseUtleder.utledOpprettetAv(null, opprettetAv = "Kelvin"))
            .isEqualTo(Bruker("Kelvin"))
    }

    @Test
    fun `opprettetAv utledes ikke fra andre historiske meldinger`() {
        val melding = OppfølgingsoppgaveV0(
            datoForOppfølging = LocalDate.of(2026, 1, 1),
            hvemSkalFølgeOpp = HvemSkalFølgeOpp.Lokalkontor,
            reserverTilBruker = null,
            opprettetAv = "legacy",
            hvaSkalFølgesOpp = "Oppfølging"
        )

        assertThat(MottattHendelseUtleder.utledOpprettetAv(melding)).isEqualTo(Bruker("legacy"))
        assertThat(MottattHendelseUtleder.utledOpprettetAv(melding, opprettetAv = "ny")).isEqualTo(Bruker("ny"))
        assertThat(melding.opprettetAv).isEqualTo("legacy")
    }

    @Test
    fun `korriger søknadsdato gir årsak KORRIGER_SØKNADSDATO og vurderingsbehov VURDER_KRAV`() {
        val melding = KorrigerSøknadsdatoV0(begrunnelse = "Feilregistrert søknadsdato")

        val årsak = MottattHendelseUtleder.utledÅrsakTilOpprettelse(InnsendingType.KORRIGER_SØKNADSDATO, melding)
        val vurderingsbehov = MottattHendelseUtleder.utledVurderingsbehov(InnsendingType.KORRIGER_SØKNADSDATO, melding)
        val beskrivelse = MottattHendelseUtleder.utledBeskrivelseForÅrsakTilOpprettelse(melding)

        assertThat(årsak).isEqualTo(ÅrsakTilOpprettelse.KORRIGER_SØKNADSDATO)
        assertThat(vurderingsbehov.map { it.type }).containsExactly(Vurderingsbehov.VURDER_KRAV)
        assertThat(beskrivelse).isEqualTo("Feilregistrert søknadsdato")
    }
}
