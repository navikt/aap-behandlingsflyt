package no.nav.aap.behandlingsflyt.behandling.trekkklage

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface TrekkKlageRepository: Repository {
    fun lagreTrekkKlageVurdering(behandlingId: BehandlingId, vurdering: TrekkKlageVurdering)
    fun hentTrekkKlageGrunnlag(behandlingId: BehandlingId): TrekkKlageGrunnlag?
}