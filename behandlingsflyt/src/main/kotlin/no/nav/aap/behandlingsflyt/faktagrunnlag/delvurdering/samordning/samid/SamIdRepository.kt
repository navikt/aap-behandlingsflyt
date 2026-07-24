package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.repository.Repository

interface SamIdRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): List<SamIdOgTpNr>
    fun lagre(behandlingId: BehandlingId, samIdOgTpNr: List<SamIdOgTpNr>)
}

data class SamIdOgTpNr(val samId: Long, val tpNr: Long?)