package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelseKafkaMelding
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class OppdaterOppgaveMedTilbakekrevingHendelseUtfører(
    private val oppgavestyringGateway: OppgavestyringGateway,
): JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<TilbakekrevingHendelseKafkaMelding>()

        log.info("Varsler hendelse til OppgaveStyring. ${hendelse.eksternFagsakId} :: ${hendelse.eksternBehandlingId}")
        oppgavestyringGateway.varsleTilbakekrevingHendelse(hendelse)
    }

    companion object : ProvidersJobbSpesifikasjon {

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OppdaterOppgaveMedTilbakekrevingHendelseUtfører(
                oppgavestyringGateway = gatewayProvider.provide(),
            )
        }

        override val beskrivelse = "Oppdater oppgave med tilbakekrevingshendelse"
        override val navn = "Oppdater oppgave med tilbakekrevingshendelse"
        override val type = "flyt.tilbakekrevingshendelse"
    }

}