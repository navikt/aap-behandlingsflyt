package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNay
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object ImMemoryKlagebehandlingNayRepository : KlagebehandlingNayRepository {
    private val vurderinger = ConcurrentHashMap<BehandlingId, KlagevurderingNay>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): KlagebehandlingNayGrunnlag? {
        return vurderinger[behandlingId]?.let { KlagebehandlingNayGrunnlag(vurdering = it) }
    }

    override fun lagre(behandlingId: BehandlingId, klagevurderingNay: KlagevurderingNay) {
        val eksisterende = hentHvisEksisterer(behandlingId)
        val nytt = KlagebehandlingNayGrunnlag(vurdering = klagevurderingNay)
        if (eksisterende != nytt) {
            vurderinger[behandlingId] = klagevurderingNay
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Gjør ingenting
    }

    override fun slett(behandlingId: BehandlingId) {
        vurderinger.remove(behandlingId)
    }
}
