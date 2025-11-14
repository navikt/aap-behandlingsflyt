package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.RepositoryProvider

class TilbakekrevingService(
    private val tilbakekrevingsbehandlingRepository: TilbakekrevingRepository
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        tilbakekrevingsbehandlingRepository = repositoryProvider.provide()
    )

    fun håndter(sakId: SakId, tilbakekrevingshendelse: Tilbakekrevingshendelse) {
        tilbakekrevingsbehandlingRepository.lagre(sakId, tilbakekrevingshendelse)
        //TODO: opprette "task" for å opprette oppgave for
    }

}