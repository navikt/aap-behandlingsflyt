package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.lovvalgMedlemskapGrunnlagAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/lovvalgmedlemskap") {
            authorizedGet<BehandlingReferanse, LovvalgMedlemskapGrunnlagDto>(
                AuthorizationParamPathConfig(behandlingPathParam = BehandlingPathParam("referanse"))
            ) { req ->
                val grunnlag = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val lovvalgMedlemskapRepository = repositoryProvider.provide<MedlemskapArbeidInntektRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    val gjeldendeManuellVurdering =
                        lovvalgMedlemskapRepository.hentHvisEksisterer(behandling.id)?.manuellVurdering
                    val historiskeManuelleVurderinger =
                        lovvalgMedlemskapRepository.hentHistoriskeVurderinger(behandling.sakId, behandling.id)

                    val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                        req.referanse,
                        Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP.kode.toString(),
                        token()
                    )

                    LovvalgMedlemskapGrunnlagDto(
                        harTilgangTilÅSaksbehandle,
                        gjeldendeManuellVurdering,
                        historiskeManuelleVurderinger
                    )
                }
                respond(grunnlag)
            }
        }
    }
}