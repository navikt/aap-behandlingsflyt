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
object OpprettBehandlingFastsattPeriodePassertJobbUtfører : JobbUtfører, ProvidersJobbSpesifikasjon {
    override fun utfør(input: JobbInput) {
    }

    override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører =
        OpprettBehandlingFastsattPeriodePassertJobbUtfører

    override val type = "batch.OpprettBehandlingFastsattPeriodePassert"
    override val navn = "Opprett behandling fordi fastsatt dag er passert"
    override val beskrivelse = """
                Starter ny behandling hvis siste behandlig har antatt at meldeplikten er oppfylt, men
                fastsatt dag er passert, og meldekort nå mangler.
            """.trimIndent()
}
