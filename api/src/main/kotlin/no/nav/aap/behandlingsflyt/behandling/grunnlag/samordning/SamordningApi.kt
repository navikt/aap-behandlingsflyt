package no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import javax.sql.DataSource

/**
 * @param ytelser Hvilke ytelser det er funnet på denne personen.
 * @param vurderinger Manuelle vurderinger gjort av saksbehandler for gitte ytelser.
 */
data class SamordningYtelseVurderingGrunnlagDTO(
    val begrunnelse: String?,
    val maksDato: LocalDate?,
    val maksDatoEndelig: Boolean?,
    val ytelser: List<SamordningYtelseDTO>,
    val vurderinger: List<SamordningVurderingDTO>,
)

data class SamordningYtelseDTO(
    val ytelseType: Ytelse,
    val periode: Periode,
    val gradering: Int?,
    val kronesum: Int?,
    val kilde: String,
    val saksRef: String?
)

data class SamordningVurderingDTO(
    val ytelseType: Ytelse,
    val periode: Periode,
    val gradering: Int?,
    val kronesum: Int?
)

data class SamordningUføreVurderingGrunnlagDTO(
    val vurdering: SamordningUføreVurderingDTO?,
    val grunnlag: List<SamordningUføreGrunnlagDTO>
)

data class SamordningUføreGrunnlagDTO(
    val virkningstidspunkt: LocalDate,
    val uføregrad: Prosent,
    val kilde: String
)

data class SamordningUføreVurderingDTO(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningUføreVurderingPeriodeDTO>,
)

data class SamordningUføreVurderingPeriodeDTO(
    val periode: Periode,
    val uføregradTilSamordning: Prosent
)


fun NormalOpenAPIRoute.samordningGrunnlag(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/samordning-ufore") {
            get<BehandlingReferanse, SamordningUføreVurderingGrunnlagDTO> { behandlingReferanse ->
                val (registerGrunnlag, vurdering) = dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val samordningUføreRepository = repositoryProvider.provide<SamordningUføreRepository>()
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val uføreRepository = repositoryProvider.provide<UføreRepository>()

                    val behandling = behandlingRepository.hent(behandlingReferanse)
                    val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
                    val samordningUføreVurdering =
                        samordningUføreRepository.hentHvisEksisterer(behandling.id)?.vurdering
                    Pair(uføreGrunnlag, samordningUføreVurdering)
                }

                respond(
                    SamordningUføreVurderingGrunnlagDTO(
                        vurdering = mapSamordningUføreVurdering(vurdering),
                        grunnlag = mapSamordningUføreGrunnlag(registerGrunnlag)

                    )
                )
            }
        }
        route("/{referanse}/grunnlag/samordning") {
            get<BehandlingReferanse, SamordningYtelseVurderingGrunnlagDTO> { req ->
                val (registerYtelser, samordning) = dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val samordningRepository = repositoryProvider.provide<SamordningVurderingRepository>()
                    val samordningYtelseRepository = repositoryProvider.provide<SamordningYtelseRepository>()

                    val behandling =
                        BehandlingReferanseService(repositoryProvider.provide<BehandlingRepository>()).behandling(req)

                    val registerYtelser = samordningYtelseRepository.hentHvisEksisterer(behandling.id)
                    val samordning = samordningRepository.hentHvisEksisterer(behandling.id)

                    Pair(registerYtelser, samordning)
                }

                respond(
                    SamordningYtelseVurderingGrunnlagDTO(
                        ytelser = registerYtelser?.ytelser?.flatMap { ytelse ->
                            ytelse.ytelsePerioder.map {
                                SamordningYtelseDTO(
                                    ytelseType = ytelse.ytelseType,
                                    periode = Periode(fom = it.periode.fom, tom = it.periode.tom),
                                    gradering = it.gradering?.prosentverdi(),
                                    kronesum = it.kronesum?.toInt(),
                                    kilde = ytelse.kilde,
                                    saksRef = ytelse.saksRef
                                )
                            }
                        }.orEmpty(),
                        vurderinger = samordning?.vurderinger.orEmpty().flatMap { vurdering ->
                            vurdering.vurderingPerioder.map {
                                SamordningVurderingDTO(
                                    ytelseType = vurdering.ytelseType,
                                    gradering = it.gradering?.prosentverdi(),
                                    periode = Periode(fom = it.periode.fom, tom = it.periode.tom),
                                    kronesum = it.kronesum?.toInt(),
                                )
                            }
                        },
                        begrunnelse = samordning?.begrunnelse,
                        maksDato = samordning?.maksDato,
                        maksDatoEndelig = samordning?.maksDatoEndelig
                    )
                )

            }
        }
    }

}

private fun mapSamordningUføreVurdering(vurdering: SamordningUføreVurdering?) : SamordningUføreVurderingDTO? {
    return vurdering?.let {
        SamordningUføreVurderingDTO(
            begrunnelse = it.begrunnelse,
            vurderingPerioder = it.vurderingPerioder.map { periode ->
                SamordningUføreVurderingPeriodeDTO(
                    periode.periode,
                    periode.uføregradTilSamordning
                )
            })
    }
}

private fun mapSamordningUføreGrunnlag(registerGrunnlag: UføreGrunnlag?): List<SamordningUføreGrunnlagDTO> {
    return registerGrunnlag?.vurderinger?.map {
        SamordningUføreGrunnlagDTO(
            virkningstidspunkt = it.virkningstidspunkt,
            uføregrad = it.uføregrad,
            kilde = it.kilde
        )
    } ?: emptyList()
}

private fun SamordningVurderingPeriode.tilDTO(): Periode {
    return Periode(
        fom = this.periode.fom,
        tom = this.periode.tom,
    )
}

private fun SamordningYtelsePeriode.tilDTO(): Periode {
    return Periode(
        fom = this.periode.fom,
        tom = this.periode.tom,
    )
}
