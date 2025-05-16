package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.MonthDay
import java.time.Year
import javax.sql.DataSource

data class ManuellInntektVurderingGrunnlagResponse(
    val begrunnelse: String,
    val vurdertAv: String,
    val tidspunkt: LocalDate,
    val ar: Int,
    val belop: BigDecimal,
)

/**
 * @param [ar] Året som det skal gjøres vurdering for. Er med i begge objektene fordi de teoretisk kan være forskjellige.
 */
data class ManuellInntektGrunnlagResponse(
    val ar: Int,
    val gVerdi: BigDecimal,
    val vurdering: ManuellInntektVurderingGrunnlagResponse?,
)

private val log = LoggerFactory.getLogger("ManuellInntektGrunnlagApi")

fun NormalOpenAPIRoute.manglendeGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/beregning/manuellinntekt") {
            authorizedGet<BehandlingReferanse, ManuellInntektGrunnlagResponse>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val (manuellInntekt, år) = dataSource.transaction {
                    val provider = repositoryRegistry.provider(it)
                    val behandlingRepository = provider.provide<BehandlingRepository>()
                    val manuellInntektRepository = provider.provide<ManuellInntektGrunnlagRepository>()
                    val beregningService = BeregningService(provider)

                    val behandling = behandlingRepository.hent(req.referanse.let(::BehandlingReferanse))
                    val relevanteÅr = beregningService.utledRelevanteBeregningsÅr(behandling.id)

                    val grunnlag = manuellInntektRepository.hentHvisEksisterer(behandling.id)
                    val manuellInntekter = grunnlag?.manuelleInntekter

                    if (manuellInntekter != null && manuellInntekter.size > 1) {
                        log.warn("Fant flere manuelle inntekter for behandling ${behandling.id}. Per nå gjør vi antakelse om kun én.")
                    }

                    Pair(manuellInntekter?.firstOrNull(), relevanteÅr.max())
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
                        gVerdi = gVerdi.verdi,
                        vurdering = manuellInntekt?.let {
                            ManuellInntektVurderingGrunnlagResponse(
                                begrunnelse = it.begrunnelse,
                                vurdertAv = it.vurdertAv,
                                tidspunkt = it.opprettet.toLocalDate(),
                                ar = it.år.value,
                                belop = it.belop.verdi,
                            )
                        }
                    ))
            }
        }
    }
}