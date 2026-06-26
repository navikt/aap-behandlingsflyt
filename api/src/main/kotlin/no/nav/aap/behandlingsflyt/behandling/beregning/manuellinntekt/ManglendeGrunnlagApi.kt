package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
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

                    val manglerInntekterFor =
                        beregningService.manglerInntekterFor(behandling.id, inkluderManuelle = false)

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
                        alleRelevanteÅr = relevanteÅr.map { it.value },
                        manglerInntektForÅr = manglerInntekterFor.map { it.value }.toList(),
                    )
                }

                respond(
                    grunnlag
                )
            }
        }
    }
}