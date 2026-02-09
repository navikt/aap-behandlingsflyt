package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OppgaveGateway
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.tilKontrakt
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingsbehandlingOppdatertHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.util.UUID

class OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører(
    val oppgaveStyringGateway: OppgavestyringGateway,
    val tilbakekrevingsbehandlingRepository: TilbakekrevingRepository,
    val sakRepository: SakRepository

): JobbUtfører {
    //TODO: HER BLIR MELDINGEN OM TILBAKEKREVING SENDT TIL OPPGAVESTYRING
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        log.info("Varsle tilbakekrevingsbehandling til oppgavestyring er ikke koblet på enda")
        return
        val tilbakekrevingSakId = SakId(input.sakId())
        val sak = sakRepository.hent(tilbakekrevingSakId)
        val tilbakekrevingBehandlingId = input.parameter("tilbakekrevingBehandlingId")
        val tilbakekrevingsbehandling = tilbakekrevingsbehandlingRepository.hent(tilbakekrevingsBehandlingId = tilbakekrevingBehandlingId)
        val tilbakekrevingsbehandlingOppdatertHendelse = TilbakekrevingsbehandlingOppdatertHendelse(
            personIdent = sak.person.aktivIdent().identifikator,
            saksnummer = sak.saksnummer,
            behandlingref = BehandlingReferanse(
                tilbakekrevingsbehandling.tilbakekrevingBehandlingId
            ),
            behandlingStatus = tilbakekrevingsbehandling.behandlingsstatus.tilKontrakt(),
            sakOpprettet = tilbakekrevingsbehandling.sakOpprettet,
            avklaringsbehov = emptyList(),
            totaltFeilutbetaltBeløp = tilbakekrevingsbehandling.totaltFeilutbetaltBeløp.verdi,
            saksbehandlingURL = tilbakekrevingsbehandling.saksbehandlingURL.toString()
        )

        oppgaveStyringGateway.varsleTilbakekrevingHendelse(tilbakekrevingsbehandlingOppdatertHendelse)



    }

    companion object : ProvidersJobbSpesifikasjon {

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører(
                oppgaveStyringGateway = gatewayProvider.provide<OppgavestyringGateway>(),
                tilbakekrevingsbehandlingRepository = repositoryProvider.provide<TilbakekrevingRepository>(),
                sakRepository = repositoryProvider.provide<SakRepository>(),
            )
        }

        override val beskrivelse = "Oppdater oppgave om tilbakekrevingsbehandling"
        override val navn = "Oppdater oppgave om tilbakekrevingsbehandling"
        override val type = "hendelse.tilbakekrevingsbehandling"
    }

}