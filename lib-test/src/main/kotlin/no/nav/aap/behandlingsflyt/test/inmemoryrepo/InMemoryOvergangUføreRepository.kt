package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.overganguføre.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.steg.overgangufore.OvergangUføreRepository
import no.nav.aap.overganguføre.OvergangUføreVurdering
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.time.LocalDateTime

object InMemoryOvergangUføreRepository: OvergangUføreRepository {
    private val mutex = Any()
    private val grunnlag = HashMap<BehandlingId, OvergangUføreGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId) = synchronized(mutex) {
        grunnlag[behandlingId]
    }

    override fun hentHistoriskeOvergangUføreVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId
    ) = emptyList<OvergangUføreVurdering>()

    override fun hentOvergangUføreVurderingPåTidspunkt(
        behandlingId: BehandlingId,
        tidspunkt: LocalDateTime
    ): List<OvergangUføreVurdering> {
        return emptyList()
    }

    override fun lagre(
        behandlingId: BehandlingId,
        overgangUføreVurderinger: List<OvergangUføreVurdering>
    )  = synchronized(mutex) {
        grunnlag[behandlingId] = OvergangUføreGrunnlag(overgangUføreVurderinger)
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) = synchronized(mutex) {
        val fraGrunnlag = grunnlag[fraBehandling]
        if (fraGrunnlag != null) {
            grunnlag[tilBehandling] = fraGrunnlag
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(mutex) {
            grunnlag.remove(behandlingId)
        }
    }
}