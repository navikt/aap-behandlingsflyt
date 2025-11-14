package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class TilbakekrevingService(
    private val sakRepository: SakRepository,
    private val tilbakekrevingsbehandlingRepository: TilbakekrevingRepository
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        sakRepository = repositoryProvider.provide(),
        tilbakekrevingsbehandlingRepository = repositoryProvider.provide()
    )

    fun håndter(sakId: SakId, tilbakekrevingshendelse: Tilbakekrevingshendelse) {
        val sak = sakRepository.hent(Saksnummer(tilbakekrevingshendelse.eksternFagsakId))
        tilbakekrevingsbehandlingRepository.lagre(sakId, tilbakekrevingshendelse)
        //TODO: opprette "task" for å opprette oppgave for
    }

}