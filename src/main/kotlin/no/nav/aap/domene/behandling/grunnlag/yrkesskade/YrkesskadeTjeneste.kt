package no.nav.aap.domene.behandling.grunnlag.yrkesskade

import no.nav.aap.domene.behandling.Behandling
import java.util.Optional
import java.util.concurrent.atomic.AtomicLong

object YrkesskadeTjeneste {

    private var grunnlagene = HashMap<Long, YrkesskadeGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun hentHvisEksisterer(behandlingId: Long): Optional<YrkesskadeGrunnlag> {
        synchronized(LOCK) {
            return Optional.ofNullable(grunnlagene[behandlingId])
        }
    }

    fun lagre(behandlingId: Long, yrkesskader: Yrkesskader?) {
        synchronized(LOCK) {
            if (yrkesskader != null) {
                grunnlagene.put(
                    behandlingId,
                    YrkesskadeGrunnlag(behandlingId = behandlingId, yrkesskader = yrkesskader, id = key.addAndGet(1))
                )
            } else {
                grunnlagene.remove(behandlingId)
            }
        }
    }

    fun kopier(fraBehandling: Behandling, tilBehandling: Behandling) {
        synchronized(LOCK) {
            hentHvisEksisterer(fraBehandling.id).ifPresent { eksisterendeGrunnlag ->
                grunnlagene[tilBehandling.id] = eksisterendeGrunnlag
            }
        }
    }
}
