package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.util.concurrent.ConcurrentHashMap

class InMemorySamordningArbeidsgiverRepository: SamordningArbeidsgiverRepository {
    private val store = ConcurrentHashMap<BehandlingId, SamordningArbeidsgiverGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningArbeidsgiverGrunnlag? {
        return store[behandlingId]
    }

    override fun lagre(
        sakId: SakId,
        behandlingId: BehandlingId,
        refusjonkravVurderinger: SamordningArbeidsgiverVurdering
    ) {
        store[behandlingId] = SamordningArbeidsgiverGrunnlag(refusjonkravVurderinger)
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        store[fraBehandling]?.let { store[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        store.remove(behandlingId)
    }

    companion object: RepositoryFactory<SamordningArbeidsgiverRepository> {
        override fun konstruer(connection: DBConnection) = InMemorySamordningArbeidsgiverRepository()
    }
}