package no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import javax.sql.DataSource

/**
 * @param ytelser Hvilke ytelser det er funnet på denne personen.
 * @param vurderinger Manuelle vurderinger gjort av saksbehandler for gitte ytelser.
 */
data class SamordningYtelseVurderingGrunnlagDTO(
    val ytelser: List<SamordningYtelseDTO>,
    val vurderinger: List<SamordningVurderingDTO>,
)

data class SamordningYtelseDTO(
    val ytelseType: Ytelse,
    val ytelsePerioder: List<SamordningYtelsePeriodeDTO>,
    val kilde: String,
    val saksRef: String?,
)

data class SamordningVurderingDTO(
    val ytelseType: Ytelse,
    val vurderingPerioder: List<SamordningVurderingPeriodeDTO>,
)

data class SamordningVurderingPeriodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
    val gradering: Int?,
    val kronesum: Int?
)

data class SamordningYtelsePeriodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
    val gradering: Int?,
    val kronesum: Int?
)

fun NormalOpenAPIRoute.samordningGrunnlag(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/samordning/") {
            get<BehandlingReferanse, SamordningYtelseVurderingGrunnlagDTO> { req ->
                val samordning = dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val samordningRepository = repositoryProvider.provide<SamordningYtelseVurderingRepository>()

                    val behandling =
                        BehandlingReferanseService(repositoryProvider.provide<BehandlingRepository>()).behandling(req)

                    val samordning = samordningRepository.hentHvisEksisterer(behandling.id)

                    samordning
                }
                if (samordning == null) {
                    respondWithStatus(HttpStatusCode.NotFound)
                } else {
                    respond(
                        SamordningYtelseVurderingGrunnlagDTO(
                            ytelser = samordning.ytelseGrunnlag.ytelser.map { it ->
                                SamordningYtelseDTO(
                                    ytelseType = it.ytelseType,
                                    ytelsePerioder = it.ytelsePerioder.map { it.tilDTO() },
                                    kilde = it.kilde,
                                    saksRef = it.saksRef
                                )
                            },
                            vurderinger = samordning.vurderingGrunnlag.vurderinger.map {
                                SamordningVurderingDTO(
                                    ytelseType = it.ytelseType,
                                    vurderingPerioder = it.vurderingPerioder.map { it.tilDTO() }
                                )
                            }
                        )
                    )
                }
            }
        }
    }
}

private fun SamordningVurderingPeriode.tilDTO(): SamordningVurderingPeriodeDTO {
    return SamordningVurderingPeriodeDTO(
        fom = this.periode.fom,
        tom = this.periode.tom,
        gradering = this.gradering?.prosentverdi(),
        kronesum = this.kronesum?.toInt()
    )
}

private fun SamordningYtelsePeriode.tilDTO(): SamordningYtelsePeriodeDTO {
    return SamordningYtelsePeriodeDTO(
        fom = this.periode.fom,
        tom = this.periode.tom,
        gradering = this.gradering?.prosentverdi(),
        kronesum = this.kronesum?.toInt()
    )
}