package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import java.util.concurrent.atomic.AtomicLong

object SykepengerErstatningRepository {

    private var grunnlagene = HashMap<Long, SykepengerErstatningGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun lagre(behandlingId: Long, vurdering: SykepengerVurdering?) {
        synchronized(LOCK) {
            grunnlagene.put(
                behandlingId,
                SykepengerErstatningGrunnlag(
                    behandlingId = behandlingId,
                    vurdering = vurdering,
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

    fun hentHvisEksisterer(behandlingId: Long): SykepengerErstatningGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    fun hent(behandlingId: Long): SykepengerErstatningGrunnlag {
        synchronized(LOCK) {
            return grunnlagene.getValue(behandlingId)
        }
    }
}
