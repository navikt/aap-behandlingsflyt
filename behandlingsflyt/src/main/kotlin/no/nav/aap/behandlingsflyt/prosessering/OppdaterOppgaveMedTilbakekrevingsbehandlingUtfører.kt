package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.tilKontrakt
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingsbehandlingOppdatertHendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.util.*

class OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører(
    val oppgaveStyringGateway: OppgavestyringGateway,
    val statistikkGateway: StatistikkGateway,
    val tilbakekrevingsbehandlingRepository: TilbakekrevingRepository,
    val unleashGateway: UnleashGateway,
    val sakRepository: SakRepository

) : JobbUtfører {
    // HER BLIR MELDINGEN OM TILBAKEKREVING SENDT TIL OPPGAVESTYRING
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sak = sakRepository.hent(SakId(input.sakId()))

        val tilbakekrevingBehandlingId = UUID.fromString(input.parameter("tilbakekrevingBehandlingId"))
        val tilbakekrevingsbehandling = tilbakekrevingsbehandlingRepository.hent(tilbakekrevingBehandlingId)
        log.info("Mottatt melding om oppdatering av oppgave med tilbakekrevingsbehandling, input: ${sak.saksnummer}")
        val tilbakekrevingsbehandlingOppdatertHendelse = TilbakekrevingsbehandlingOppdatertHendelse(
            personIdent = sak.person.aktivIdent().identifikator,
            saksnummer = sak.saksnummer,
            behandlingref = BehandlingReferanse(
                tilbakekrevingsbehandling.tilbakekrevingBehandlingId
            ),
            behandlingStatus = tilbakekrevingsbehandling.behandlingsstatus.tilKontrakt(),
            sakOpprettet = tilbakekrevingsbehandling.sakOpprettet,
            totaltFeilutbetaltBeløp = tilbakekrevingsbehandling.totaltFeilutbetaltBeløp.verdi,
            saksbehandlingURL = tilbakekrevingsbehandling.saksbehandlingURL.toString()
        )

        log.info("Kaller oppgavestyring for å varsle om oppdatering av tilbakekrevingsbehandling for sak: ${sak.saksnummer}")
        oppgaveStyringGateway.varsleTilbakekrevingHendelse(tilbakekrevingsbehandlingOppdatertHendelse)
        statistikkGateway.varsleTilbakekrevingHendelse(tilbakekrevingsbehandlingOppdatertHendelse)
    }

    companion object : ProvidersJobbSpesifikasjon {

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører(
                oppgaveStyringGateway = gatewayProvider.provide<OppgavestyringGateway>(),
                tilbakekrevingsbehandlingRepository = repositoryProvider.provide<TilbakekrevingRepository>(),
                unleashGateway = gatewayProvider.provide<UnleashGateway>(),
                statistikkGateway = gatewayProvider.provide<StatistikkGateway>(),
                sakRepository = repositoryProvider.provide<SakRepository>(),
            )
        }

        override val beskrivelse = "Oppdater oppgave om tilbakekrevingsbehandling"
        override val navn = "Oppdater oppgave om tilbakekrevingsbehandling"
        override val type = "hendelse.tilbakekrevingsbehandling"
    }

}