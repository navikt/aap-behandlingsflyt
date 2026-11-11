package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlag
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryOppholdskravGrunnlagRepository : OppholdskravGrunnlagRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, OppholdskravGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): OppholdskravGrunnlag? = grunnlag[behandlingId]

    override fun lagre(behandlingId: BehandlingId, oppholdskravVurdering: OppholdskravVurdering) {
        grunnlag.compute(behandlingId) { _, eksisterende ->
            val tidligereVurderinger = eksisterende?.vurderinger.orEmpty().filter {
                it.vurdertIBehandling != behandlingId
            }
            OppholdskravGrunnlag(
                tidligereVurderinger + oppholdskravVurdering.copy(
                    vurdertIBehandling = behandlingId,
                    perioder = oppholdskravVurdering.perioder.toList()
                )
            )
        }
    }

    override fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandling: BehandlingId?) {
        require(behandlingId != forrigeBehandling)
        grunnlag.remove(behandlingId)
        forrigeBehandling?.let { kopier(it, behandlingId) }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        grunnlag[fraBehandling]?.let { grunnlag[tilBehandling] = OppholdskravGrunnlag(it.vurderinger) }
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}
