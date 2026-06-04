package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon

/** Vi antar ikke lenger at meldeplikt kan være oppfylt i fremtiden. Denne jobben
 *   endret "FREMTIDIG_OPPFYLT" til ikke oppfylt når fristen var passert, for å forhindre
 *   at det lå planlagte utbetalinger når meldeplikten ikke var oppfylt.
 **/
object OpprettJobbForFastsattPeriodeJobbUtfører : JobbUtfører, ProvidersJobbSpesifikasjon {
    override fun utfør(input: JobbInput) {
    }

    override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
        OpprettJobbForFastsattPeriodeJobbUtfører

    override val type = "batch.OpprettJobbForFastsattPeriode"
    override val navn = "Start jobb for å sjekke behov for revurdering pga manglende meldekort"
    override val beskrivelse = """Start jobb for å sjekke om fastsatt dager er passert."""
}
