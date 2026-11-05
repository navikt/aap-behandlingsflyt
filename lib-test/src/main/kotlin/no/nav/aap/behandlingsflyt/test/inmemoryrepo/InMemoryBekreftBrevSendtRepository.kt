package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.utenlandsopphold.BekreftBrevSendtBekreftelse
import no.nav.aap.utenlandsopphold.BekreftBrevSendtGrunnlag
import no.nav.aap.utenlandsopphold.BekreftBrevSendtRepository
import java.util.concurrent.ConcurrentHashMap

object InMemoryBekreftBrevSendtRepository : BekreftBrevSendtRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, BekreftBrevSendtGrunnlag>()

    override fun lagre(behandlingId: BehandlingId, bekreftelse: BekreftBrevSendtBekreftelse) {
        grunnlag[behandlingId] = BekreftBrevSendtGrunnlag(bekreftelse)
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BekreftBrevSendtGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Grunnlaget kopieres ikke, som i produksjonsimplementasjonen
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}
