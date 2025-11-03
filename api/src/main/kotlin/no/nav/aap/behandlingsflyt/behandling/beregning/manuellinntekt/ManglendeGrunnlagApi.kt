package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.MonthDay
import java.time.Year
import javax.sql.DataSource

data class ManuellInntektVurderingGrunnlagResponse(
    val begrunnelse: String,
    val vurdertAv: VurdertAvResponse,
    val ar: Int,
    val belop: BigDecimal,
)

/**
 * @param [ar] Året som det skal gjøres vurdering for. Er med i begge objektene fordi de teoretisk kan være forskjellige.
 */
data class ManuellInntektGrunnlagResponse(
    val ar: Int,
    val gverdi: BigDecimal,
    val vurdering: ManuellInntektVurderingGrunnlagResponse?,
    val historiskeVurderinger: List<ManuellInntektVurderingGrunnlagResponse>,
    val harTilgangTilÅSaksbehandle: Boolean,
)

private val log = LoggerFactory.getLogger("ManuellInntektGrunnlagApi")

fun NormalOpenAPIRoute.manglendeGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/beregning/manuellinntekt") {
            getGrunnlag<BehandlingReferanse, ManuellInntektGrunnlagResponse>(
                relevanteIdenterResolver =  relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.FASTSETT_MANUELL_INNTEKT.kode.toString()
            ) { req ->
                val (manuellInntekt, år, historiskeVurderinger) = dataSource.transaction {
                    val provider = repositoryRegistry.provider(it)
                    val behandlingRepository = provider.provide<BehandlingRepository>()
                    val beregningService = BeregningService(provider)
                    val manuellInntektRepository = provider.provide<ManuellInntektGrunnlagRepository>()

                    val behandling = behandlingRepository.hent(req.referanse.let(::BehandlingReferanse))
                    val relevanteÅr = beregningService.utledRelevanteBeregningsÅr(behandling.id)

                    val grunnlag = manuellInntektRepository.hentHvisEksisterer(behandling.id)
                    val manuellInntekter = grunnlag?.manuelleInntekter

                    if (manuellInntekter != null && manuellInntekter.size > 1) {
                        log.warn("Fant flere manuelle inntekter for behandling ${behandling.id}. Per nå gjør vi antakelse om kun én.")
                    }

                    val historiskeVurderinger =
                        manuellInntektRepository.hentHistoriskeVurderinger(behandling.sakId, behandling.id)

                    Triple(manuellInntekter?.firstOrNull(), relevanteÅr.max(), historiskeVurderinger)
                }

                // Gjennomsnittlig G-verdi første januar i året vi er interessert i
                val gVerdi = Grunnbeløp.tilTidslinjeGjennomsnitt().segment(
                    Year.of(år.value).atMonthDay(
                        MonthDay.of(1, 1)
                    )
                )!!.verdi

                respond(
                    ManuellInntektGrunnlagResponse(
                        ar = år.value,
                        gverdi = gVerdi.verdi,
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = manuellInntekt?.let {
                            ManuellInntektVurderingGrunnlagResponse(
                                begrunnelse = it.begrunnelse,
                                vurdertAv = VurdertAvResponse(it.vurdertAv, it.opprettet.toLocalDate()),
                                ar = it.år.value,
                                belop = it.belop.verdi,
                            )
                        },
                        historiskeVurderinger = historiskeVurderinger.map {
                            ManuellInntektVurderingGrunnlagResponse(
                                begrunnelse = it.begrunnelse,
                                vurdertAv = VurdertAvResponse(it.vurdertAv, it.opprettet.toLocalDate()),
                                ar = it.år.value,
                                belop = it.belop.verdi,
                            )
                        }
                    ))
            }
        }
    }
}