package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class ArbeidsopptrappingRepositoryImpl(private val connection: DBConnection): ArbeidsopptrappingRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsopptrappingGrunnlag? {
        TODO("Not yet implemented")
    }

    override fun hentAlleVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId
    ): Set<ArbeidsopptrappingVurdering> {
        TODO("Not yet implemented")
    }

    override fun lagre(
        behandlingId: BehandlingId,
        arbeidsopptrappingVurdering: List<ArbeidsopptrappingVurdering>
    ) {
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