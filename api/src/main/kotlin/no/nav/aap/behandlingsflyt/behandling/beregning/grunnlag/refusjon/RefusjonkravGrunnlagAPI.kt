package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.navenheter.NavKontorService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

fun NormalOpenAPIRoute.refusjonGrunnlagAPI(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/refusjon") {
            authorizedGet<BehandlingReferanse, RefusjonkravGrunnlagResponse>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val response =
                    dataSource.transaction(readOnly = true) { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val refusjonkravRepository = repositoryProvider.provide<RefusjonkravRepository>()

                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                        val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                        val gjeldendeVurderinger =
                            refusjonkravRepository.hentHvisEksisterer(behandling.id)?.map { it.tilResponse() }
                        val historiskeVurderinger =
                            refusjonkravRepository
                                .hentAlleVurderingerPåSak(
                                    behandling.sakId
                                ).map { it.tilResponse() }

                        val harTilgangTilÅSaksbehandle =
                            GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
                                req.referanse,
                                Definisjon.REFUSJON_KRAV,
                                token()
                            )

                        RefusjonkravGrunnlagResponse(
                            harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                            gjeldendeVurderinger = gjeldendeVurderinger,
                            historiskeVurderinger = historiskeVurderinger
                        )
                    }
                respond(response)
            }
        }
    }

    route("/api/navenhet") {
        route("/{referanse}/finn") {
            authorizedPost<BehandlingReferanse, List<NavEnheterResponse>, NavEnheterRequest>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req, body ->
                val response =
                    NavKontorService().hentNavEnheter()?.filter { enhet -> enhet.navn.contains(body.navn, ignoreCase = true) }
                ?.map { enhet ->
                    NavEnheterResponse(navn = enhet.navn, enhetsnummer = enhet.enhetsNummer)
                } ?: emptyList()
                respond(response)
            }
        }
    }
}


private fun RefusjonkravVurdering.tilResponse(): RefusjonkravVurderingResponse {
    val navnOgEnhet = AnsattInfoService().hentAnsattNavnOgEnhet(vurdertAv)
    return RefusjonkravVurderingResponse(
        harKrav = harKrav,
        navKontor = navKontor,
        fom = fom,
        tom = tom,
        vurdertAv =
            VurdertAvResponse(
                ident = vurdertAv,
                dato = opprettetTid?.toLocalDate() ?: error("Fant ikke opprettet tid for refusjonkrav vurdering"),
                ansattnavn = navnOgEnhet?.navn,
                enhetsnavn = navnOgEnhet?.enhet
            )
    )
}