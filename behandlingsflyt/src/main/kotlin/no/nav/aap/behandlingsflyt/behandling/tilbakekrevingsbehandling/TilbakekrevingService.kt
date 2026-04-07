package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.oppgave.EnhetNrDto
import no.nav.aap.behandlingsflyt.pip.PipService
import no.nav.aap.behandlingsflyt.prosessering.OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører
import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.FagsysteminfoSvarHendelse
import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.SendFagsysteminfoBehovTilTilbakekrevingUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput

class TilbakekrevingService(
    private val tilbakekrevingsbehandlingRepository: TilbakekrevingRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val oppgavestyringGateway: OppgavestyringGateway,
    private val pipService: PipService,
) {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        tilbakekrevingsbehandlingRepository = repositoryProvider.provide(),
        flytJobbRepository = repositoryProvider.provide(),
        oppgavestyringGateway = gatewayProvider.provide(),
        pipService = PipService(repositoryProvider),
    )

    fun håndter(sakId: SakId, tilbakekrevingshendelse: Tilbakekrevingshendelse) {
        tilbakekrevingsbehandlingRepository.lagre(sakId, tilbakekrevingshendelse)
        flytJobbRepository.leggTil(
            jobbInput = JobbInput(jobb = OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører)
                .medParameter("tilbakekrevingBehandlingId", tilbakekrevingshendelse.tilbakekrevingBehandlingId.toString())
                .forSak(sakId = sakId.toLong())
         )
    }

    fun håndter(sakId: SakId, hendelse: FagsysteminfoSvarHendelse) {
        flytJobbRepository.leggTil(
            jobbInput = JobbInput(jobb = SendFagsysteminfoBehovTilTilbakekrevingUtfører)
                .medPayload(hendelse)
                .forSak(sakId = sakId.toLong())
        )
    }

    fun finnNayEnhetForPerson(personIdent: Ident, behandling: Behandling): EnhetNrDto {
        val relevanteIdenter = pipService.finnIdenterPåBehandling(behandling.referanse).map { it.ident }.distinct()
        return oppgavestyringGateway.finnNayEnhetForPerson(personIdent = personIdent.identifikator, relevanteIdenter = relevanteIdenter)
    }

}
