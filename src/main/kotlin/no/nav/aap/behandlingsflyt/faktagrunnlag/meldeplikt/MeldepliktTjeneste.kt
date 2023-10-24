package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import java.util.concurrent.atomic.AtomicLong

object MeldepliktTjeneste {

    private var grunnlagene = HashMap<Long, MeldepliktGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun lagre(behandlingId: Long, vurderinger: List<Fritaksvurdering>) {
        synchronized(LOCK) {
            grunnlagene.put(
                behandlingId,
                MeldepliktGrunnlag(
                    behandlingId = behandlingId,
                    vurderinger = vurderinger,
                    id = key.addAndGet(1)
                )
            )
        }
    }

    fun kopier(fraBehandling: Behandling, tilBehandling: Behandling) {
        synchronized(LOCK) {
            grunnlagene[fraBehandling.id]?.let { eksisterendeGrunnlag ->
                grunnlagene[tilBehandling.id] = eksisterendeGrunnlag
            }
        }
    }

    fun hentHvisEksisterer(behandlingId: Long): MeldepliktGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    fun hent(behandlingId: Long): MeldepliktGrunnlag {
        synchronized(LOCK) {
            return grunnlagene.getValue(behandlingId)
        }
    }
}
