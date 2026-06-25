package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.behandling.beregning.UføreInntektUtleder
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDate
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
                    val inntektRepository = provider.provide<InntektGrunnlagRepository>()
                    val beregningVurderingRepository = provider.provide<BeregningVurderingRepository>()
                    val uføreRepository = provider.provide<UføreRepository>()
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

                    val manglendeInntektGrunnlagService =
                        ManglendeInntektGrunnlagService(repositoryRegistry.provider(it))

                    val mappedVurdering = manglendeInntektGrunnlagService.mapManuellVurderinger(req, vurdertAvService)
                    val mappedNyHistorikk =
                        manglendeInntektGrunnlagService.mapHistoriskeManuelleVurderinger(req, vurdertAvService)

                    val år = relevanteÅr.max()

                    ManuellInntektGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        manuelleVurderinger = mappedVurdering,
                        historiskeManuelleVurderinger = mappedNyHistorikk,
                        registrerteInntekterSisteRelevanteAr = registrerteInntekterSisteTreÅr,
                        sisteRelevanteÅr = år.value,
                        manglendeMånedsInntekter = utledManglendeMånedsperioderForSplittÅr(
                            unleashGateway = gatewayProvider.provide(),
                            ytterligereNedsattDato = beregningVurderingRepository.hentHvisEksisterer(behandling.id)?.tidspunktVurdering?.ytterligereNedsattArbeidsevneDato,
                            uføregrader = uføreRepository.hentHvisEksisterer(behandling.id)?.vurderinger.orEmpty(),
                            inntektGrunnlag = inntektGrunnlag,
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
 * Månedsperioder (uføregrad-segmenter) for år der uføregraden endrer seg midt i året, slik at
 * saksbehandler må legge inn beregnet PGI per delperiode.
 */
private fun utledManglendeMånedsperioderForSplittÅr(
    unleashGateway: UnleashGateway,
    ytterligereNedsattDato: LocalDate?,
    uføregrader: Set<Uføre>,
    inntektGrunnlag: InntektGrunnlag?,
): List<MånedsperiodeData> {
    if (!unleashGateway.isEnabled(BehandlingsflytFeature.ManuellInntektDelvisUfore)) return emptyList()
    if (ytterligereNedsattDato == null || uføregrader.isEmpty() || inntektGrunnlag == null) return emptyList()

    return UføreInntektUtleder.finnÅrSomKreverManuellPeriodeinntekt(
        uføregrader = uføregrader,
        inntektPerMåned = inntektGrunnlag.inntektPerMåned,
        årsInntekter = inntektGrunnlag.inntekter,
        ytterligereNedsattDato = ytterligereNedsattDato,
    ).flatMap { år ->
        UføreInntektUtleder.utledDelperioder(uføregrader, år).map { delperiode ->
            MånedsperiodeData(
                år = år.value,
                periode = Periode(delperiode.periode.fom, delperiode.periode.tom),
                uføregrad = delperiode.uføregrad.prosentverdi(),
            )
        }
    }
}