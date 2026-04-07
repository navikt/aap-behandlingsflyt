package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryArbeidsopptrappingRepository : ArbeidsopptrappingRepository {
    override fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsopptrappingGrunnlag? {
        TODO("Not yet implemented")
    }

    override fun lagre(
        behandlingId: BehandlingId,
        arbeidsopptrappingVurderinger: List<ArbeidsopptrappingVurdering>
    ) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO("Not yet implemented")
    }
}