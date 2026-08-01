package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.overgangarbeid.OvergangArbeidGrunnlag
import no.nav.aap.behandlingsflyt.steg.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.behandling.BehandlingId
import java.time.LocalDateTime

object InMemoryOvergangArbeidRepository : OvergangArbeidRepository {
    private val mutex = Any()
    private val grunnlag = HashMap<BehandlingId, OvergangArbeidGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId) =
        synchronized(mutex) { grunnlag[behandlingId] }

    override fun hentOvergangArbeidVurderingPåTidspunkt(
        behandlingId: BehandlingId,
        tidspunkt: LocalDateTime
    ): List<OvergangArbeidVurdering>? = null

    override fun lagre(
        behandlingId: BehandlingId,
        overgangArbeidVurderinger: List<OvergangArbeidVurdering>
    ) = synchronized(mutex) {
        grunnlag[behandlingId] = OvergangArbeidGrunnlag(overgangArbeidVurderinger)
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    )  = synchronized(mutex) {
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