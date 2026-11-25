package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KorrigerSøknadsdatoV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadUtenlandsoppholdV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MottattHendelseUtlederTest {

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

    @Test
    fun `søknad om opphold i utlandet gir egen årsak og eget vurderingsbehov`() {
        val melding = SøknadUtenlandsoppholdV0(mottattDato = LocalDate.of(2025, 1, 15))

        val årsak = MottattHendelseUtleder.utledÅrsakTilOpprettelse(InnsendingType.SØKNAD_UTENLANDSOPPHOLD, melding)
        val vurderingsbehov =
            MottattHendelseUtleder.utledVurderingsbehov(InnsendingType.SØKNAD_UTENLANDSOPPHOLD, melding)

        assertThat(årsak).isEqualTo(ÅrsakTilOpprettelse.SØKNAD_UTENLANDSOPPHOLD)
        assertThat(vurderingsbehov.map { it.type }).containsExactly(Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
    }
}
