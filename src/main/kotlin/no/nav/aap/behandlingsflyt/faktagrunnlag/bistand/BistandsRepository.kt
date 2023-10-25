package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand

import no.nav.aap.behandlingsflyt.avklaringsbehov.bistand.BistandsVurdering
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import java.util.concurrent.atomic.AtomicLong

object BistandsRepository {

    private var grunnlagene = HashMap<Long, BistandsGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun lagre(behandlingId: Long, bistandsVurdering: BistandsVurdering) {
        synchronized(LOCK) {
            grunnlagene.put(
                behandlingId,
                BistandsGrunnlag(
                    behandlingId = behandlingId,
                    vurdering = bistandsVurdering,
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

    fun hentHvisEksisterer(behandlingId: Long): BistandsGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    fun hent(behandlingId: Long): BistandsGrunnlag {
        synchronized(LOCK) {
            return grunnlagene.getValue(behandlingId)
        }
    }
}
