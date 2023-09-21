package no.nav.aap.domene.behandling.grunnlag.sykdom

import no.nav.aap.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.domene.behandling.Behandling
import java.util.concurrent.atomic.AtomicLong

object SykdomsTjeneste {

    private var grunnlagene = HashMap<Long, SykdomsGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun lagre(behandlingId: Long, yrkesskadevurdering: Yrkesskadevurdering?, sykdomsvurdering: Sykdomsvurdering) {
        synchronized(LOCK) {
            grunnlagene.put(
                behandlingId,
                SykdomsGrunnlag(
                    behandlingId = behandlingId,
                    yrkesskadevurdering = yrkesskadevurdering,
                    sykdomsvurdering = sykdomsvurdering,
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

    fun hentHvisEksisterer(behandlingId: Long): SykdomsGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    fun hent(behandlingId: Long): SykdomsGrunnlag {
        synchronized(LOCK) {
            return grunnlagene.getValue(behandlingId)
        }
    }
}
