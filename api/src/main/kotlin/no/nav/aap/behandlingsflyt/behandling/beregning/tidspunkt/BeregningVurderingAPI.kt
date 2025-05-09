package no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.beregningVurderingAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/beregning/tidspunkt") {
            authorizedGet<BehandlingReferanse, BeregningTidspunktAvklaringDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val responsDto = dataSource.transaction(readOnly = true) {
                    val repositoryProvider = repositoryRegistry.provider(it)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val uføreRepository = repositoryProvider.provide<UføreRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    // Dette er logikk, burde i egen service
                    val skalVurdereUføre =
                        uføreRepository.hentHvisEksisterer(behandling.id)?.vurderinger?.isNotEmpty() == true
                    val beregningGrunnlag =
                        repositoryProvider.provide<BeregningVurderingRepository>()
                            .hentHvisEksisterer(behandlingId = behandling.id)

                    val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                        req.referanse,
                        Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT.kode.toString(),
                        token()
                    )


                    BeregningTidspunktAvklaringDto(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        vurdering = beregningGrunnlag?.tidspunktVurdering,
                        skalVurdereYtterligere = skalVurdereUføre
                    )
                }

                respond(responsDto)
            }
        }
        route("/{referanse}/grunnlag/beregning/yrkesskade") {
            authorizedGet<BehandlingReferanse, BeregningYrkesskadeAvklaringDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val responsDto = dataSource.transaction(readOnly = true) {
                    val repositoryProvider = repositoryRegistry.provider(it)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
                    val yrkesskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()

                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val yrkesskadevurdering =
                        sykdomRepository.hentHvisEksisterer(behandling.id)?.yrkesskadevurdering
                    val registerYrkeskade =
                        yrkesskadeRepository.hentHvisEksisterer(behandling.id)?.yrkesskader?.yrkesskader
                            ?: emptyList()
                    val beregningGrunnlag =
                        repositoryProvider.provide<BeregningVurderingRepository>()
                            .hentHvisEksisterer(behandlingId = behandling.id)

                    val relevanteSaker = yrkesskadevurdering?.relevanteSaker ?: emptyList()
                    val sakerMedDato =
                        relevanteSaker.map { sak -> registerYrkeskade.singleOrNull { it.ref == sak } }

                    yrkesskadevurdering?.relevanteSaker

                    val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                        req.referanse,
                        Definisjon.FASTSETT_YRKESSKADEINNTEKT.kode.toString(),
                        token()
                    )


                    BeregningYrkesskadeAvklaringDto(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        skalVurderes = sakerMedDato.filterNotNull().map {
                            YrkesskadeTilVurdering(
                                it.ref, it.skadedato,
                                Grunnbeløp.finnGUnit(it.skadedato, Beløp(1)).beløp
                            )
                        },
                        vurderinger = beregningGrunnlag?.yrkesskadeBeløpVurdering?.vurderinger ?: emptyList()
                    )
                }

                respond(responsDto)
            }
        }
    }
}