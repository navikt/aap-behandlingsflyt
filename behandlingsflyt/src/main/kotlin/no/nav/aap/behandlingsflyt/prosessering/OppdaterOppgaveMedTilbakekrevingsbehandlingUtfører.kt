package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører(
): JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        log.info("Varsle tilbakekrevingsbehandling til oppgavestyring er ikke koblet på enda")
    }

    companion object : ProvidersJobbSpesifikasjon {

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører(
            )
        }

        override val beskrivelse = "Oppdater oppgave om tilbakekrevingsbehandling"
        override val navn = "Oppdater oppgave om tilbakekrevingsbehandling"
        override val type = "hendelse.tilbakekrevingsbehandling"
    }

}