package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

object InMemorySykdomsvurderingForBrevRepository : SykdomsvurderingForBrevRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, SykdomsvurderingForBrev>()
    private val historikk = ConcurrentHashMap<BehandlingId, List<SykdomsvurderingForBrev>>()

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: SykdomsvurderingForBrev
    ) {
        val nyVurdering = vurdering.copy(behandlingId = behandlingId)
        grunnlag[behandlingId] = nyVurdering
        historikk.compute(behandlingId) { _, tidligere -> tidligere.orEmpty() + nyVurdering }
    }

    override fun hent(behandlingId: BehandlingId): SykdomsvurderingForBrev? {
        return grunnlag[behandlingId]
    }

    override fun hentSykdomsvurderingForBrevPåTidspunkt(
        behandlingId: BehandlingId,
        tidspunkt: LocalDateTime
    ): SykdomsvurderingForBrev? {
        return historikk[behandlingId]
            ?.asReversed()
            ?.filter { it.vurdertTidspunkt <= tidspunkt }
            ?.maxByOrNull { it.vurdertTidspunkt }
    }

    override fun hent(sakId: SakId): List<SykdomsvurderingForBrev> {
        return grunnlag.values.filter { InMemoryBehandlingRepository.hent(it.behandlingId).sakId == sakId }
    }

    override fun deaktiverEksisterende(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
        historikk.remove(behandlingId)
    }
}