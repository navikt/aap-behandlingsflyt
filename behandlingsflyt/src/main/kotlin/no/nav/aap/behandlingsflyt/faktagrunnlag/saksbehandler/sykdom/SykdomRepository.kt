package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface SykdomRepository : Repository {
    fun lagre(
        behandlingId: BehandlingId,
        sykdomsvurderinger: List<Sykdomsvurdering>,
    )

    fun lagre(
        behandlingId: BehandlingId,
        yrkesskadevurdering: Yrkesskadevurdering?
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): SykdomGrunnlag?
    fun hent(behandlingId: BehandlingId): SykdomGrunnlag

    fun hentHistoriskeSykdomsvurderinger(sakId: SakId, behandlingId: BehandlingId): List<Sykdomsvurdering>

    fun hentBehandlingIderMedUmigrerteSykdomsvurderinger(sisteBehandlingId: Long): List<BehandlingId>

    fun oppdaterNyeFelter(
        sykdomVurderingId: Long,
        erNedsettelseMinstHalvparten: ErNedsettelseMinstHalvpartenValg?,
        erNedsettelseMerEnnYrkesskadegrense: ErNedsettelseMerEnnYrkesskadegrenseValg?
    )

    fun hentSykdomsvurderingMedId(behandlingId: BehandlingId): List<SykdomsvurderingMedId>
}

data class SykdomsvurderingMedId(
    val id: Long,
    val sykdomsvurdering: Sykdomsvurdering
)
