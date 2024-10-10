package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksperioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.meldepliktsgrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/fritak-meldeplikt") {
        get<BehandlingReferanse, FritakMeldepliktGrunnlagDto> { req ->
            val meldepliktGrunnlag = dataSource.transaction { connection ->
                val behandling: Behandling =
                    BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                val meldepliktRepository = MeldepliktRepository(connection)
                val nåTilstand = meldepliktRepository.hentHvisEksisterer(behandling.id)?.vurderinger

                if (nåTilstand == null) {
                    return@transaction null
                }
                val vedtatteVerdier =
                    behandling.forrigeBehandlingId?.let { meldepliktRepository.hentHvisEksisterer(it) }?.vurderinger
                        ?: emptyList()
                val historikk = meldepliktRepository.hentAlleVurderinger(behandling.sakId)

                FritakMeldepliktGrunnlagDto(
                    historikk = historikk.map { tilDto(it) }.sortedBy { it.vurderingsTidspunkt }.toSet(),
                    gjeldendeVedtatteVurderinger = vedtatteVerdier.map { tilDto(it) }
                        .sortedBy { it.fraDato },
                    vurderinger = nåTilstand.filterNot { vedtatteVerdier.contains(it) }.map { tilDto(it) }
                        .sortedBy { it.fraDato }
                )
            }

            if (meldepliktGrunnlag != null) {
                respond(meldepliktGrunnlag)
            } else {
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }
    }
    route("/api/behandling/{referanse}/grunnlag/fritak-meldeplikt/simulering") {
        post<BehandlingReferanse, SimulertFritakMeldepliktDto, SimulerFritakMeldepliktDto> { req, dto ->
            val meldepliktGrunnlag = dataSource.transaction { connection ->
                val behandling: Behandling =
                    BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                val meldepliktRepository = MeldepliktRepository(connection)

                val vedtatteVerdier =
                    behandling.forrigeBehandlingId?.let { meldepliktRepository.hentHvisEksisterer(it) }?.vurderinger
                        ?: emptyList()

                val eksisterendeFritak = Fritaksperioder(vedtatteVerdier)
                val simulertFritak =
                    eksisterendeFritak.leggTil(Fritaksperioder(dto.fritaksvurderinger.map { it.toFritaksvurdering() }))

                SimulertFritakMeldepliktDto(simulertFritak.gjeldendeFritaksvurderinger().map { tilDto(it) })
            }

            respond(meldepliktGrunnlag)
        }
    }
}

private fun tilDto(fritaksvurdering: Fritaksvurdering): FritakMeldepliktVurderingDto {
    return FritakMeldepliktVurderingDto(
        fritaksvurdering.begrunnelse,
        requireNotNull(fritaksvurdering.opprettetTid),
        fritaksvurdering.harFritak,
        fritaksvurdering.fraDato
    )
}