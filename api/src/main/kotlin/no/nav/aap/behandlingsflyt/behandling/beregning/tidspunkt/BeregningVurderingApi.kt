package no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattNavnOgEnhet
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
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.beregningVurderingApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling") {
        route("/{referanse}/grunnlag/beregning/tidspunkt") {
            getGrunnlag<BehandlingReferanse, BeregningTidspunktAvklaringResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT.kode.toString()
            ) { req ->
                val responsDto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val uføreRepository = repositoryProvider.provide<UføreRepository>()
                    val beregningVurderingRepository = repositoryProvider.provide<BeregningVurderingRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    // Dette er logikk, burde i egen service
                    val skalVurdereUføre =
                        uføreRepository.hentHvisEksisterer(behandling.id)?.vurderinger?.isNotEmpty() == true

                    val beregningGrunnlag =
                        beregningVurderingRepository.hentHvisEksisterer(behandlingId = behandling.id)
                    val historiskeVurderinger =
                        beregningVurderingRepository.hentHistoriskeVurderinger(behandling.sakId, behandling.id)

                    BeregningTidspunktAvklaringResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = beregningGrunnlag?.tidspunktVurdering?.tilResponse(ansattInfoService),
                        historiskeVurderinger = historiskeVurderinger.mapNotNull {
                            it.tidspunktVurdering?.tilResponse(ansattInfoService)
                        },
                        skalVurdereYtterligere = skalVurdereUføre
                    )
                }

                respond(responsDto)
            }
        }
        route("/{referanse}/grunnlag/beregning/yrkesskade") {
            getGrunnlag<BehandlingReferanse, BeregningYrkesskadeAvklaringResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.FASTSETT_YRKESSKADEINNTEKT.kode.toString()
            ) { req ->
                val responsDto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
                    val yrkesskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()
                    val beregningVurderingRepository = repositoryProvider.provide<BeregningVurderingRepository>()

                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val yrkesskadevurdering =
                        sykdomRepository.hentHvisEksisterer(behandling.id)?.yrkesskadevurdering
                    val registerYrkeskade =
                        yrkesskadeRepository.hentHvisEksisterer(behandling.id)?.yrkesskader?.yrkesskader.orEmpty()
                    val beregningGrunnlag = beregningVurderingRepository
                        .hentHvisEksisterer(behandlingId = behandling.id)
                    val historiskeVurderinger = beregningVurderingRepository
                        .hentHistoriskeVurderinger(behandling.sakId, behandling.id)

                    val relevanteSaker = yrkesskadevurdering?.relevanteSaker.orEmpty()
                    val sakerMedDato =
                        relevanteSaker.map { sak -> registerYrkeskade.singleOrNull { it.ref == sak.referanse } }

                    BeregningYrkesskadeAvklaringResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        skalVurderes =
                            sakerMedDato.filterNotNull().map {
                                val skadedato = requireNotNull(
                                    it.skadedato
                                        ?: yrkesskadevurdering?.relevanteSaker?.firstOrNull { sak -> sak.referanse == it.ref }?.manuellYrkesskadeDato
                                )
                                YrkesskadeTilVurderingResponse(
                                    it.ref,
                                    it.saksnummer,
                                    it.kildesystem,
                                    skadedato,
                                    Grunnbeløp.finnGUnit(skadedato, Beløp(1)).beløp
                                )
                            },
                        vurderinger =
                            beregningGrunnlag
                                ?.yrkesskadeBeløpVurdering
                                ?.vurderinger
                                ?.sortedByDescending { vurdering -> vurdering.vurdertTidspunkt }
                                ?.map { vurdering ->
                                    vurdering.toResponse(
                                        ansattInfoService.hentAnsattNavnOgEnhet(
                                            vurdering.vurdertAv
                                        )
                                    )
                                }
                                .orEmpty(),
                        historiskeVurderinger = historiskeVurderinger
                            .mapNotNull { it.yrkesskadeBeløpVurdering }
                            .flatMap { it.vurderinger }
                            .map { it.toResponse(ansattInfoService.hentAnsattNavnOgEnhet(it.vurdertAv)) }
                    )
                }

                respond(responsDto)
            }
        }
    }
}

private fun BeregningstidspunktVurdering.tilResponse(ansattInfoService: AnsattInfoService): BeregningstidspunktVurderingResponse {
    val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(vurdertAv)
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


private fun YrkesskadeBeløpVurdering.toResponse(navnOgEnhet: AnsattNavnOgEnhet?): YrkesskadeBeløpVurderingResponse {
    return YrkesskadeBeløpVurderingResponse(
        antattÅrligInntekt = antattÅrligInntekt,
        referanse = referanse,
        begrunnelse = begrunnelse,
        vurdertAv =
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