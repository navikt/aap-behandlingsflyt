package no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
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
            authorizedGet<BehandlingReferanse, BeregningTidspunktAvklaringResponse>(
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

                    val harTilgangTilÅSaksbehandle =
                        GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
                            req.referanse,
                            Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
                            token()
                        )


                    BeregningTidspunktAvklaringResponse(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        vurdering = beregningGrunnlag?.tidspunktVurdering?.tilResponse(),
                        skalVurdereYtterligere = skalVurdereUføre
                    )
                }

                respond(responsDto)
            }
        }
        route("/{referanse}/grunnlag/beregning/yrkesskade") {
            authorizedGet<BehandlingReferanse, BeregningYrkesskadeAvklaringResponse>(
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

                    val harTilgangTilÅSaksbehandle =
                        GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
                            req.referanse,
                            Definisjon.FASTSETT_YRKESSKADEINNTEKT,
                            token()
                        )

                    BeregningYrkesskadeAvklaringResponse(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        skalVurderes =
                            sakerMedDato.filterNotNull().map {
                                YrkesskadeTilVurderingResponse(
                                    it.ref,
                                    it.skadedato,
                                    Grunnbeløp.finnGUnit(it.skadedato, Beløp(1)).beløp
                                )
                            },
                        vurderinger =
                            beregningGrunnlag?.yrkesskadeBeløpVurdering?.vurderinger?.map { it.toResponse() }
                                ?: emptyList()
                    )
                }

                respond(responsDto)
            }
        }
    }
}

private fun BeregningstidspunktVurdering.tilResponse(): BeregningstidspunktVurderingResponse {
    val navnOgEnhet = AnsattInfoService().hentAnsattNavnOgEnhet(vurdertAv)
    return BeregningstidspunktVurderingResponse(
        begrunnelse = begrunnelse,
        nedsattArbeidsevneDato = nedsattArbeidsevneDato,
        ytterligereNedsattBegrunnelse = ytterligereNedsattBegrunnelse,
        ytterligereNedsattArbeidsevneDato = ytterligereNedsattArbeidsevneDato,
        vurdertAv = VurdertAvResponse(
            ident = vurdertAv,
            dato = requireNotNull(vurdertTidspunkt?.toLocalDate()) { "Fant ikke vurdert tidspunkt for beregningstidspunktvurdering" },
            ansattnavn = navnOgEnhet?.navn,
            enhetsnavn = navnOgEnhet?.enhet
        )
    )
}


private fun YrkesskadeBeløpVurdering.toResponse(): YrkesskadeBeløpVurderingResponse {
    val navnOgEnhet = AnsattInfoService().hentAnsattNavnOgEnhet(vurdertAv)
    return YrkesskadeBeløpVurderingResponse(
        antattÅrligInntekt = antattÅrligInntekt,
        referanse = referanse,
        begrunnelse = begrunnelse,
        vurdertAvResponse =
            VurdertAvResponse(
                ident = vurdertAv,
                dato =
                    requireNotNull(
                        vurdertTidspunkt?.toLocalDate()
                    ) { "Fant ikke vurdert tidspunkt for yrkesskadebeløpvurdering" },
                ansattnavn = navnOgEnhet?.navn,
                enhetsnavn = navnOgEnhet?.enhet
            )
    )
}