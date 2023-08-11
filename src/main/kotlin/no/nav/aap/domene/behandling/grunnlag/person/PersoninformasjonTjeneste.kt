package no.nav.aap.domene.behandling.grunnlag.person

import no.nav.aap.domene.behandling.Behandling
import java.util.concurrent.atomic.AtomicLong

object PersoninformasjonTjeneste {

    private var grunnlagene = HashMap<Long, PersoninfoGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun hentHvisEksisterer(behandlingId: Long): PersoninfoGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    fun lagre(behandlingId: Long, personinfo: Personinfo?) {
        synchronized(LOCK) {
            if (personinfo != null) {
                grunnlagene.put(
                    behandlingId,
                    PersoninfoGrunnlag(key.addAndGet(1L), personinfo)
                )
            } else {
                grunnlagene.remove(behandlingId)
            }
        }
    }

    fun kopier(fraBehandling: Behandling, tilBehandling: Behandling) {
        synchronized(LOCK) {
            val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling.id)

            if (eksisterendeGrunnlag != null) {
                grunnlagene[tilBehandling.id] = eksisterendeGrunnlag
            }
        }
    }
}
