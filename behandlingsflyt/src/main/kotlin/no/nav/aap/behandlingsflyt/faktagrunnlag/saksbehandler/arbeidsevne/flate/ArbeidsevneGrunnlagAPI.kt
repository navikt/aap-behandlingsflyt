package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevnePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

fun NormalOpenAPIRoute.arbeidsevneGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/{referanse}/grunnlag/arbeidsevne") {
        authorizedGet<BehandlingReferanse, ArbeidsevneGrunnlagDto>(
            AuthorizationParamPathConfig(
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                )
            )
        ) { behandlingReferanse ->

            arbeidsevneGrunnlag(dataSource, behandlingReferanse, token(), repositoryRegistry)?.let { respond(it) } ?: respondWithStatus(
                HttpStatusCode.NoContent
            )
        }

        route("/simulering") {
            authorizedPost<BehandlingReferanse, SimulertArbeidsevneResultatDto, SimulerArbeidsevneDto>(
                AuthorizationParamPathConfig(behandlingPathParam = BehandlingPathParam("referanse"))
            ) { behandlingReferanse, dto ->
                respond(simuleringsresulat(dataSource, behandlingReferanse, dto, repositoryRegistry))
            }
        }

    }
}

private fun arbeidsevneGrunnlag(
    dataSource: DataSource,
    behandlingReferanse: BehandlingReferanse,
    token: OidcToken,
    repositoryRegistry: RepositoryRegistry
): ArbeidsevneGrunnlagDto? {
    return dataSource.transaction { connection ->
        val repositoryProvider = repositoryRegistry.provider(connection)
        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
        val behandling: Behandling =
            BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
        val arbeidsevneRepository = repositoryProvider.provide<ArbeidsevneRepository>()

        val nåTilstand = arbeidsevneRepository.hentHvisEksisterer(behandling.id)?.vurderinger

        val vedtatteVerdier =
            behandling.forrigeBehandlingId?.let { arbeidsevneRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
        val historikk = arbeidsevneRepository.hentAlleVurderinger(behandling.sakId, behandling.id)

        val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
            behandlingReferanse.referanse,
            Definisjon.FASTSETT_ARBEIDSEVNE.kode.toString(),
            token
        )

        ArbeidsevneGrunnlagDto(
            harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
            historikk = historikk.map { it.toDto() }.sortedBy { it.vurderingsTidspunkt }.toSet(),
            vurderinger = nåTilstand?.filterNot { vedtatteVerdier.contains(it) }?.map { it.toDto() }
                ?.sortedBy { it.fraDato } ?: emptyList(),
            gjeldendeVedtatteVurderinger = vedtatteVerdier.map { it.toDto() }
                .sortedBy { it.fraDato }
        )
    }
}

private fun simuleringsresulat(
    dataSource: DataSource,
    behandlingReferanse: BehandlingReferanse,
    dto: SimulerArbeidsevneDto,
    repositoryRegistry: RepositoryRegistry
): SimulertArbeidsevneResultatDto {
    return dataSource.transaction(readOnly = true) { con ->
        val repositoryProvider = repositoryRegistry.provider(con)
        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
        val arbeidsevneRepository = repositoryProvider.provide<ArbeidsevneRepository>()
        val behandling =
            BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)

        val vedtatteArbeidsevner =
            behandling.forrigeBehandlingId?.let { arbeidsevneRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
        val nåværendeArbeidsevnePerioder = ArbeidsevnePerioder(vedtatteArbeidsevner)
        val simuleringsresultat = nåværendeArbeidsevnePerioder.leggTil(
            ArbeidsevnePerioder(dto.vurderinger.map { it.toArbeidsevnevurdering() })
        )

        SimulertArbeidsevneResultatDto(
            simuleringsresultat.gjeldendeArbeidsevner().map { it.toDto() })
    }
}