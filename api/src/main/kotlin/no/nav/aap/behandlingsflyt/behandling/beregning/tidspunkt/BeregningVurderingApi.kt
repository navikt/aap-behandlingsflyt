package no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.SkadekombinasjonRegister
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
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
    route("/api/behandling") {
        route("/{referanse}/grunnlag/beregning/tidspunkt") {
            getGrunnlag<BehandlingReferanse, BeregningTidspunktAvklaringResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                påkrevdRolle = Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT.løsesAv
            ) { req ->
                val responsDto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val uføreRepository = repositoryProvider.provide<UføreRepository>()
                    val beregningVurderingRepository = repositoryProvider.provide<BeregningVurderingRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val sak = repositoryProvider.provide<SakRepository>().hent(behandling.sakId)

                    // Dette er logikk, burde i egen service
                    val uføreTidslinje = uføreRepository.hentHvisEksisterer(behandling.id)?.vurderinger?.tilTidslinje()
                    val skalVurdereUføre = uføreTidslinje?.isNotEmpty() == true
                            && uføreTidslinje.segment(sak.rettighetsperiode.fom) != null

                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                    val beregningGrunnlag =
                        beregningVurderingRepository.hentHvisEksisterer(behandlingId = behandling.id)
                    val historiskeVurderinger = behandlingRepository.hentAlleFor(
                        behandling.sakId,
                        TypeBehandling.ytelseBehandlingstyper()
                    ).filter { it.id != behandling.id }
                        .mapNotNull { historiskBehandling ->
                            beregningVurderingRepository.hentHvisEksisterer(historiskBehandling.id)?.let {
                                historiskBehandling.id to it
                            }
                        }

                    BeregningTidspunktAvklaringResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = beregningGrunnlag?.tidspunktVurdering?.tilResponse(vurdertAvService, behandling.id),
                        historiskeVurderinger = historiskeVurderinger.mapNotNull { (behandlingId, grunnlag) ->
                            grunnlag.tidspunktVurdering?.tilResponse(vurdertAvService, behandlingId)
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
                påkrevdRolle = Definisjon.FASTSETT_YRKESSKADEINNTEKT.løsesAv
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
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                    val beregningGrunnlag = beregningVurderingRepository
                        .hentHvisEksisterer(behandlingId = behandling.id)
                    val historiskeVurderinger = behandlingRepository.hentAlleFor(
                        behandling.sakId,
                        TypeBehandling.ytelseBehandlingstyper()
                    ).filter { it.id != behandling.id }
                        .mapNotNull { historiskBehandling ->
                            beregningVurderingRepository.hentHvisEksisterer(historiskBehandling.id)?.let {
                                historiskBehandling.id to it
                            }
                        }

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
                                    it.vedtaksdato,
                                    it.skadeart,
                                    it.diagnose,
                                    it.skadekombinasjoner,
                                    it.skadekombinasjonerTekst,
                                    Grunnbeløp.finnGUnit(skadedato, Beløp(1)).beløp
                                )
                            },
                        vurderinger =
                            beregningGrunnlag
                                ?.yrkesskadeBeløpVurdering
                                ?.vurderinger
                                ?.sortedByDescending { vurdering -> vurdering.vurdertTidspunkt }
                                ?.map { vurdering ->
                                    vurdering.toResponse(vurdertAvService, behandling.id)
                                }
                                .orEmpty(),
                        historiskeVurderinger = historiskeVurderinger
                            .mapNotNull { (behandlingId, grunnlag) ->
                                grunnlag.yrkesskadeBeløpVurdering?.let { behandlingId to it }
                            }
                            .flatMap { (behandlingId, vurdering) ->
                                vurdering.vurderinger.map { it.toResponse(vurdertAvService, behandlingId) }
                            }
                    )
                }

                respond(responsDto)
            }
        }
    }
}

private fun BeregningstidspunktVurdering.tilResponse(
    vurdertAvService: VurdertAvService,
    behandlingId: BehandlingId,
): BeregningstidspunktVurderingResponse {
    return BeregningstidspunktVurderingResponse(
        begrunnelse = begrunnelse,
        nedsattArbeidsevneDato = nedsattArbeidsevneEllerStudieevneDato,
        ytterligereNedsattBegrunnelse = ytterligereNedsattBegrunnelse,
        ytterligereNedsattArbeidsevneDato = ytterligereNedsattArbeidsevneDato,
        vurderingerMeta = vurdertAvService.byggVurderingerMeta(
            definisjon = Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
            behandlingId = behandlingId,
            vurdertAv = vurdertAvService.medNavnOgEnhet(
                ident = vurdertAv,
                dato = requireNotNull(vurdertTidspunkt?.toLocalDate()) {
                    "Fant ikke vurdert tidspunkt for beregningstidspunktvurdering"
                },
            ),
        ),
    )
}


private fun YrkesskadeBeløpVurdering.toResponse(
    vurdertAvService: VurdertAvService,
    behandlingId: BehandlingId,
): YrkesskadeBeløpVurderingResponse {
    return YrkesskadeBeløpVurderingResponse(
        antattÅrligInntekt = antattÅrligInntekt,
        referanse = referanse,
        begrunnelse = begrunnelse,
        vurderingerMeta = vurdertAvService.byggVurderingerMeta(
            definisjon = Definisjon.FASTSETT_YRKESSKADEINNTEKT,
            behandlingId = behandlingId,
            vurdertAv = vurdertAvService.medNavnOgEnhet(
                ident = vurdertAv,
                dato =
                    requireNotNull(
                        vurdertTidspunkt?.toLocalDate()
                    ) { "Fant ikke vurdert tidspunkt for yrkesskadebeløpvurdering" },
            ),
        ),
    )
}