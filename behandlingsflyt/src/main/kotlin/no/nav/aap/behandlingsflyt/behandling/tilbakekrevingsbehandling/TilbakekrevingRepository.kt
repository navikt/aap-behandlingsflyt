package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import java.util.*

interface TilbakekrevingRepository : Repository {
    fun lagre(sakId: SakId, tilbakekrevingshendelse: Tilbakekrevingshendelse)
    fun hent(sakId: SakId): List<Tilbakekrevingsbehandling>
    fun hent(tilbakekrevingsBehandlingId: UUID): Tilbakekrevingsbehandling
}