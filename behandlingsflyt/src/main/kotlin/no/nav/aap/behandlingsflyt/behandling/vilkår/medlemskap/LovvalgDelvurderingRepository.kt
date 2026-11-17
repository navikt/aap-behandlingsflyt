package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface LovvalgDelvurderingRepository : Repository {

    fun hentAlle(behandlingId: BehandlingId): List<LovvalgDelvurderingerGrunnlag>

    /** Delvurderingen for den nyeste relevante datoen. */
    fun hentHvisEksisterer(behandlingId: BehandlingId): LovvalgDelvurderingerGrunnlag? =
        hentAlle(behandlingId).maxByOrNull { it.relevantDato }

    /** Erstatter hele settet med delvurderinger. Det skrives ikke ny historikk dersom settet er uendret. */
    fun lagre(behandlingId: BehandlingId, delvurderinger: List<LovvalgDelvurderingerGrunnlag>)

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
