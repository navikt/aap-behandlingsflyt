package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.utenlandsopphold.SøknadUtenlandsoppholdGrunnlag
import no.nav.aap.utenlandsopphold.SøknadUtenlandsoppholdRepository
import no.nav.aap.utenlandsopphold.SøknadUtenlandsoppholdVurdering
import java.util.concurrent.ConcurrentHashMap

object InMemorySøknadUtenlandsoppholdRepository : SøknadUtenlandsoppholdRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, SøknadUtenlandsoppholdGrunnlag>()

    override fun lagre(behandlingId: BehandlingId, vurdering: SøknadUtenlandsoppholdVurdering) {
        grunnlag[behandlingId] = SøknadUtenlandsoppholdGrunnlag(vurdering)
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SøknadUtenlandsoppholdGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Grunnlaget kopieres ikke, som i produksjonsimplementasjonen
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}
