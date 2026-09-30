package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryKlagebehandlingKontorRepository : KlagebehandlingKontorRepository {
    private val memory = HashMap<BehandlingId, KlagebehandlingKontorGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): KlagebehandlingKontorGrunnlag? {
        return memory[behandlingId]
    }

    override fun lagre(
        behandlingId: BehandlingId,
        klagevurderingKontor: KlagevurderingKontor
    ) {
        memory[behandlingId] = KlagebehandlingKontorGrunnlag(klagevurderingKontor)
    }

    override fun deaktiverEksisterende(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }

}
