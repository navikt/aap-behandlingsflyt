package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.util.concurrent.ConcurrentHashMap

object InMemorySykdomRepository : SykdomRepository {

    private val sykdomsvurderingerMap = ConcurrentHashMap<BehandlingId, List<Sykdomsvurdering>>()
    private val yrkesskadevurderingMap = ConcurrentHashMap<BehandlingId, Yrkesskadevurdering>()

    override fun lagre(
        behandlingId: BehandlingId,
        sykdomsvurderinger: List<Sykdomsvurdering>
    ) {
        sykdomsvurderingerMap[behandlingId] = sykdomsvurderinger
    }

    override fun lagre(
        behandlingId: BehandlingId,
        yrkesskadevurdering: Yrkesskadevurdering?
    ) {
        if (yrkesskadevurdering != null) {
            yrkesskadevurderingMap[behandlingId] = yrkesskadevurdering
        } else {
            yrkesskadevurderingMap.remove(behandlingId)
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        sykdomsvurderingerMap[fraBehandling]?.let {
            sykdomsvurderingerMap[tilBehandling] = it
        }
        yrkesskadevurderingMap[fraBehandling]?.let {
            yrkesskadevurderingMap[tilBehandling] = it
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SykdomGrunnlag? {
        val sykdomsvurderinger = sykdomsvurderingerMap[behandlingId]
        val yrkesskadevurdering = yrkesskadevurderingMap[behandlingId]

        if (sykdomsvurderinger == null && yrkesskadevurdering == null) {
            return null
        }

        return SykdomGrunnlag(
            yrkesskadevurdering = yrkesskadevurdering,
            sykdomsvurderinger = sykdomsvurderinger ?: emptyList()
        )
    }

    override fun hent(behandlingId: BehandlingId): SykdomGrunnlag {
        return hentHvisEksisterer(behandlingId)
            ?: throw IllegalStateException("Fant ikke sykdomsgrunnlag for behandling $behandlingId")
    }

    override fun hentHistoriskeSykdomsvurderinger(
        sakId: SakId,
        behandlingId: BehandlingId
    ): List<Sykdomsvurdering> {
        // Returnerer alle sykdomsvurderinger som er lagret på behandlingen, unntatt de som er vurdert i denne behandlingen
        return sykdomsvurderingerMap[behandlingId]?.filter {
            it.vurdertIBehandling != behandlingId
        } ?: emptyList()
    }

    override fun slett(behandlingId: BehandlingId) {
        sykdomsvurderingerMap.remove(behandlingId)
        yrkesskadevurderingMap.remove(behandlingId)
    }
}