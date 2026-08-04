package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.time.LocalDateTime

class InMemoryArbeidsevneRepository: ArbeidsevneRepository {
    override fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsevneGrunnlag? = TODO()

    override fun hentArbeidsevneVurderingPåTidspunkt(
        behandlingId: BehandlingId,
        tidspunkt: LocalDateTime
    ): List<ArbeidsevneVurdering>? = null

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: List<ArbeidsevneVurdering>
    ) {
        TODO()
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO()
    }

    override fun slett(behandlingId: BehandlingId) {
        TODO()
    }

    companion object: RepositoryFactory<ArbeidsevneRepository> {
        override fun konstruer(connection: DBConnection) = InMemoryArbeidsevneRepository()
    }
}