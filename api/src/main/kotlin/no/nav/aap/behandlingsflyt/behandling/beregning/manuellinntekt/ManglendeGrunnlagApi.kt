package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
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

data class ManuellInntektGrunnlagResponse(
    val begrunnelse: String,
    val vurdertAv: String,
    val tidspunkt: LocalDate,
    val ar: Int,
    val belop: BigDecimal,
    val gVerdi: BigDecimal
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
                val manuellInntekt = dataSource.transaction {
                    val provider = repositoryRegistry.provider(it)
                    val behandlingRepository = provider.provide<BehandlingRepository>()
                    val manuellInntektRepository = provider.provide<ManuellInntektGrunnlagRepository>()

                    val behandling = behandlingRepository.hent(req.referanse.let(::BehandlingReferanse))


                    val grunnlag = manuellInntektRepository.hentHvisEksisterer(behandling.id)
                    val manuellInntekter = grunnlag?.manuelleInntekter

                    if (manuellInntekter != null && manuellInntekter.size > 1) {
                        log.warn("Fant flere manuelle inntekter for behandling ${behandling.id}. Per nå gjør vi antakelse om kun én.")
                    }

                    manuellInntekter?.firstOrNull()
                }


                if (manuellInntekt != null) {
                    // Gjennomsnittlig G-verdi første januar i året vi er interessert i
                    val gVerdi = Grunnbeløp.tilTidslinjeGjennomsnitt().segment(
                        Year.of(manuellInntekt.år.value).atMonthDay(
                            MonthDay.of(1, 1)
                        )
                    )!!.verdi

                    respond(
                        ManuellInntektGrunnlagResponse(
                            begrunnelse = manuellInntekt.begrunnelse,
                            vurdertAv = manuellInntekt.vurdertAv,
                            tidspunkt = manuellInntekt.opprettet.toLocalDate(),
                            ar = manuellInntekt.år.value,
                            belop = manuellInntekt.belop.verdi,
                            gVerdi = gVerdi.verdi,
                        )
                    )
                } else {
                    respondWithStatus(HttpStatusCode.NoContent)
                }
            }
        }
    }
}