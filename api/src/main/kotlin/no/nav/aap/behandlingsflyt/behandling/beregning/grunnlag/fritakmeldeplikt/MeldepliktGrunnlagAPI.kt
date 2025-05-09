package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.fritakmeldeplikt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktFritaksperioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import java.time.LocalDateTime
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldepliktsgrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/{referanse}/grunnlag/fritak-meldeplikt") {
        authorizedGet<BehandlingReferanse, FritakMeldepliktGrunnlagDto>(
            AuthorizationParamPathConfig(behandlingPathParam = BehandlingPathParam("referanse"))
        ) { req ->
            val meldepliktGrunnlag = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val meldepliktRepository = repositoryProvider.provide<MeldepliktRepository>()

                val behandling: Behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(req)

                val nåTilstand = meldepliktRepository.hentHvisEksisterer(behandling.id)?.vurderinger

                val vedtatteVerdier =
                    behandling.forrigeBehandlingId?.let { meldepliktRepository.hentHvisEksisterer(it) }?.vurderinger
                        ?: emptyList()
                val historikk =
                    meldepliktRepository.hentAlleVurderinger(behandling.sakId, behandling.id)

                val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                    req.referanse,
                    Definisjon.FRITAK_MELDEPLIKT.kode.toString(),
                    token()
                )

                FritakMeldepliktGrunnlagDto(
                    harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                    historikk = historikk.map { tilDto(it) }.sortedBy { it.vurderingsTidspunkt }
                        .toSet(),
                    gjeldendeVedtatteVurderinger = vedtatteVerdier.map { tilDto(it) }
                        .sortedBy { it.fraDato },
                    vurderinger = nåTilstand?.filterNot { vedtatteVerdier.contains(it) }
                        ?.map { tilDto(it) }
                        ?.sortedBy { it.fraDato } ?: emptyList(),
                )
            }

            respond(meldepliktGrunnlag)
        }
    }
    route("/api/behandling/{referanse}/grunnlag/fritak-meldeplikt/simulering") {
        authorizedPost<BehandlingReferanse, SimulertFritakMeldepliktDto, SimulerFritakMeldepliktDto>(
            routeConfig = AuthorizationParamPathConfig(behandlingPathParam = BehandlingPathParam("referanse"))
        ) { req, dto ->
            val meldepliktGrunnlag = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val meldepliktRepository = repositoryProvider.provide<MeldepliktRepository>()
                val behandling: Behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(req)

                val vedtatteVerdier =
                    behandling.forrigeBehandlingId?.let { meldepliktRepository.hentHvisEksisterer(it) }?.vurderinger
                        ?: emptyList()

                val eksisterendeFritak = MeldepliktFritaksperioder(vedtatteVerdier)
                val simulertFritak =
                    eksisterendeFritak.leggTil(MeldepliktFritaksperioder(dto.fritaksvurderinger.map { it.toFritaksvurdering() }))

                SimulertFritakMeldepliktDto(
                    simulertFritak.gjeldendeFritaksvurderinger().map { tilDto(it) })
            }

            respond(meldepliktGrunnlag)
        }
    }
}

private fun tilDto(fritaksvurdering: Fritaksvurdering): FritakMeldepliktVurderingDto {
    return FritakMeldepliktVurderingDto(
        fritaksvurdering.begrunnelse,
        fritaksvurdering.opprettetTid ?: LocalDateTime.now(),
        fritaksvurdering.harFritak,
        fritaksvurdering.fraDato
    )
}