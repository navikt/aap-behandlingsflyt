package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryAvbrytAktivitetspliktbehandlingRepository : AvbrytAktivitetspliktbehandlingRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, AvbrytAktivitetspliktbehandlingGrunnlag>()

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: AvbrytAktivitetspliktbehandlingVurdering
    ) {
        grunnlag[behandlingId] = AvbrytAktivitetspliktbehandlingGrunnlag(
            vurdering = vurdering
        )
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): AvbrytAktivitetspliktbehandlingGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        // Skal ikke gjøres
    }

    override fun slett(behandlingId: BehandlingId) {
        // Skal ikke gjøres
    }
}