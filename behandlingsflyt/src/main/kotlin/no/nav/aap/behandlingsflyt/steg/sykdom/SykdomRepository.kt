package no.nav.aap.behandlingsflyt.steg.sykdom

import java.time.LocalDateTime
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.sykdom.SykdomGrunnlag
import no.nav.aap.sykdom.Sykdomsvurdering
import no.nav.aap.sykdom.Yrkesskadevurdering

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

    fun hentSykdomsvurderingerPåTidspunkt(behandlingId: BehandlingId, tidspunkt: LocalDateTime): List<Sykdomsvurdering>?
}