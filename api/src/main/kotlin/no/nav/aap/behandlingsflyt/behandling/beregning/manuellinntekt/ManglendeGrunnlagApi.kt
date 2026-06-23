package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.behandling.beregning.UføreInntektUtleder
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.math.BigDecimal
import java.time.MonthDay
import java.time.Year
import javax.sql.DataSource

fun NormalOpenAPIRoute.manglendeGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/beregning/manuellinntekt") {
            getGrunnlag<BehandlingReferanse, ManuellInntektGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                påkrevdRolle = Definisjon.FASTSETT_MANUELL_INNTEKT.løsesAv
            ) { req ->
                val grunnlag = dataSource.transaction {
                    val provider = repositoryRegistry.provider(it)
                    val behandlingRepository = provider.provide<BehandlingRepository>()
                    val beregningService = BeregningService(provider)
                    val manuellInntektRepository = provider.provide<ManuellInntektGrunnlagRepository>()
                    val inntektRepository = provider.provide<InntektGrunnlagRepository>()
                    val vurdertAvService = VurdertAvService(provider, gatewayProvider)

                    val behandling = behandlingRepository.hent(req.referanse.let(::BehandlingReferanse))
                    val relevanteÅr = beregningService.utledRelevanteBeregningsÅr(behandling.id)

                    val inntektGrunnlag = inntektRepository.hentHvisEksisterer(behandling.id)
                    val registrerteInntekterSisteTreÅr = inntektGrunnlag?.inntekter?.filter { inntekt ->
                        relevanteÅr.contains(inntekt.år)
                    }.orEmpty().map { inntekt ->
                        ÅrData(
                            år = inntekt.år.value,
                            beløp = inntekt.beløp.verdi
                        )
                    }

                    val grunnlag = manuellInntektRepository.hentHvisEksisterer(behandling.id)
                    val manuellInntekter = grunnlag?.manuelleInntekter

                    val gamleHistoriske =
                        manuellInntektRepository.hentHistoriskeVurderinger(behandling.sakId, behandling.id)
                            .flatten()

                    val manglendeInntektGrunnlagService =
                        ManglendeInntektGrunnlagService(repositoryRegistry.provider(it))

                    val mappedVurdering = manglendeInntektGrunnlagService.mapManuellVurderinger(req, vurdertAvService)
                    val mappedNyHistorikk =
                        manglendeInntektGrunnlagService.mapHistoriskeManuelleVurderinger(req, vurdertAvService)

                    // Gjennomsnittlig G-verdi første januar i året vi er interessert i
                    val gVerdi = Grunnbeløp.gjennomsnittGrunnbeløp(
                        Year.of(relevanteÅr.max().value).atMonthDay(
                            MonthDay.of(1, 1)
                        )
                    )

                    val år = relevanteÅr.max()
                    val gammelVurderingFormat = manuellInntekter?.firstOrNull()

                    ManuellInntektGrunnlagResponse(
                        ar = år.value,
                        gverdi = gVerdi.verdi,
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = gammelVurderingFormat?.let { vurdering ->
                            ManuellInntektVurderingGrunnlagResponse(
                                begrunnelse = vurdering.begrunnelse,
                                vurdertAv = VurdertAvResponse(
                                    vurdering.vurdertAv,
                                    vurdering.opprettet.toLocalDate()
                                ),
                                ar = vurdering.år.value,
                                belop = vurdering.belop?.verdi ?: BigDecimal.ZERO,
                            )
                        },
                        historiskeVurderinger = gamleHistoriske.map { vurdering ->
                            ManuellInntektVurderingGrunnlagResponse(
                                begrunnelse = vurdering.begrunnelse,
                                vurdertAv = VurdertAvResponse(
                                    vurdering.vurdertAv,
                                    vurdering.opprettet.toLocalDate()
                                ),
                                ar = vurdering.år.value,
                                belop = vurdering.belop?.verdi ?: BigDecimal.ZERO,
                            )
                        },
                        manuelleVurderinger = mappedVurdering,
                        historiskeManuelleVurderinger = mappedNyHistorikk,
                        registrerteInntekterSisteRelevanteAr = registrerteInntekterSisteTreÅr,
                        sisteRelevanteÅr = år.value,
                        delperioderForSplittÅr = utledDelperioderForSplittÅr(
                            repositoryRegistry.provider(it), gatewayProvider, behandling.id
                        ),
                    )
                }

                respond(
                    grunnlag
                )
            }
        }
    }
}

/**
 * Delperioder (uføregrad-segmenter) for år der uføregraden endrer seg midt i året, slik at
 * saksbehandler må legge inn beregnet PGI per delperiode.
 */
private fun utledDelperioderForSplittÅr(
    provider: RepositoryProvider,
    gatewayProvider: GatewayProvider,
    behandlingId: BehandlingId,
): List<DelperiodeData> {
    val unleash = gatewayProvider.provide<UnleashGateway>()
    if (!unleash.isEnabled(BehandlingsflytFeature.ManuellInntektDelvisUfore)) return emptyList()

    val ytterligereNedsattDato = provider.provide<BeregningVurderingRepository>().hentHvisEksisterer(behandlingId)
        ?.tidspunktVurdering?.ytterligereNedsattArbeidsevneDato
    val uføregrader = provider.provide<UføreRepository>().hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty()
    val inntektGrunnlag = provider.provide<InntektGrunnlagRepository>().hentHvisEksisterer(behandlingId)
    if (ytterligereNedsattDato == null || uføregrader.isEmpty() || inntektGrunnlag == null) return emptyList()

    return UføreInntektUtleder.finnÅrSomKreverManuellPeriodeinntekt(
        uføregrader = uføregrader,
        inntektPerMåned = inntektGrunnlag.inntektPerMåned,
        årsInntekter = inntektGrunnlag.inntekter,
        ytterligereNedsattDato = ytterligereNedsattDato,
    ).flatMap { år ->
        UføreInntektUtleder.utledDelperioder(uføregrader, år).map { delperiode ->
            DelperiodeData(
                år = år.value,
                periodeFom = delperiode.periode.fom,
                periodeTom = delperiode.periode.tom,
                uføregrad = delperiode.uføregrad.prosentverdi(),
            )
        }
    }
}