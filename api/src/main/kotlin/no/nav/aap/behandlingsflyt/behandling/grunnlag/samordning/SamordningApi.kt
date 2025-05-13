package no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.samordning.EndringStatus
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningPeriodeSammenligner
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.AndreStatligeYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UførePeriodeMedEndringStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UførePeriodeSammenligner
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
import javax.sql.DataSource

/**
 * @param ytelser Hvilke ytelser det er funnet på denne personen.
 * @param vurderinger Manuelle vurderinger gjort av saksbehandler for gitte ytelser.
 */
data class SamordningYtelseVurderingGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val begrunnelse: String?,
    val maksDato: LocalDate?,
    val maksDatoEndelig: Boolean?,
    val ytelser: List<SamordningYtelseDTO>,
    val vurderinger: List<SamordningVurderingDTO>,
    val tpYtelser: List<TjenestePensjonForhold>?,
)

data class SamordningYtelseDTO(
    val ytelseType: Ytelse,
    val periode: Periode,
    val gradering: Int?,
    val kronesum: Int?,
    val kilde: String,
    val saksRef: String?,
    val endringStatus: EndringStatus
)

data class SamordningVurderingDTO(
    val ytelseType: Ytelse,
    val periode: Periode,
    val gradering: Int?,
    val kronesum: Int?,
    val manuell: Boolean?
)

data class SamordningUføreVurderingGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: SamordningUføreVurderingDTO?,
    val grunnlag: List<SamordningUføreGrunnlagDTO>
)

data class SamordningUføreGrunnlagDTO(
    val virkningstidspunkt: LocalDate,
    val uføregrad: Int,
    val kilde: String,
    val endringStatus: EndringStatus,
)

data class SamordningUføreVurderingDTO(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningUføreVurderingPeriodeDTO>,
)

data class SamordningUføreVurderingPeriodeDTO(
    val virkningstidspunkt: LocalDate,
    val uføregradTilSamordning: Int
)

data class SamordningAndreStatligeYtelserGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: SamordningAndreStatligeYtelserVurderingDTO?,
)

data class SamordningAndreStatligeYtelserVurderingDTO(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningAndreStatligeYtelserVurderingPeriodeDTO>,
)

data class SamordningAndreStatligeYtelserVurderingPeriodeDTO(
    val periode: Periode,
    val ytelse: AndreStatligeYtelser,
    val beløp: Int
)


fun NormalOpenAPIRoute.samordningGrunnlag(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/samordning-ufore") {
            authorizedGet<BehandlingReferanse, SamordningUføreVurderingGrunnlagDTO>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            )
            { behandlingReferanse ->
                val (registerGrunnlag, vurdering) = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val samordningUføreRepository = repositoryProvider.provide<SamordningUføreRepository>()
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val uføreRepository = repositoryProvider.provide<UføreRepository>()

                    val behandling = behandlingRepository.hent(behandlingReferanse)
                    val samordningUføreVurdering =
                        samordningUføreRepository.hentHvisEksisterer(behandling.id)?.vurdering
                    val uføregrunnlagMedEndretStatus =
                        UførePeriodeSammenligner(uføreRepository).hentUføreGrunnlagMedEndretStatus(behandling.id)

                    Pair(uføregrunnlagMedEndretStatus, samordningUføreVurdering)
                }

                val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                    behandlingReferanse.referanse,
                    Definisjon.AVKLAR_SAMORDNING_UFØRE.kode.toString(),
                    token()
                )


                respond(
                    SamordningUføreVurderingGrunnlagDTO(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        vurdering = mapSamordningUføreVurdering(vurdering),
                        grunnlag = mapSamordningUføreGrunnlag(registerGrunnlag)

                    )
                )
            }
        }
        route("/{referanse}/grunnlag/samordning/tjenestepensjon") {
            authorizedGet<BehandlingReferanse, TjenestePensjonGrunnlag>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { req ->
                val tp = dataSource.transaction{ connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val tjenestePensjonRepository = repositoryProvider.provide<TjenestePensjonRepository>()
                    val behandling =
                        BehandlingReferanseService(repositoryProvider.provide<BehandlingRepository>()).behandling(req)
                    tjenestePensjonRepository.hent(behandling.id)
                }
                respond(TjenestePensjonGrunnlag(tp))
            }
        }

        route("/{referanse}/grunnlag/samordning") {
            authorizedGet<BehandlingReferanse, SamordningYtelseVurderingGrunnlagDTO>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            )
            { req ->
                val (registerYtelser, samordning, tp) = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val samordningRepository = repositoryProvider.provide<SamordningVurderingRepository>()
                    val samordningYtelseRepository = repositoryProvider.provide<SamordningYtelseRepository>()
                    val tjenestePensjonRepository = repositoryProvider.provide<TjenestePensjonRepository>()

                    val behandling =
                        BehandlingReferanseService(repositoryProvider.provide<BehandlingRepository>()).behandling(req)

                    val samordning = samordningRepository.hentHvisEksisterer(behandling.id)

                    val perioderMedEndringer =
                        SamordningPeriodeSammenligner(samordningYtelseRepository).hentPerioderMarkertMedEndringer(
                            behandling.id
                        )

                    val tp = tjenestePensjonRepository.hentHvisEksisterer(behandling.id)

                    Triple(perioderMedEndringer, samordning, tp)
                }

                val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                    req.referanse,
                    Definisjon.AVKLAR_SAMORDNING_GRADERING.kode.toString(),
                    token()
                )

                respond(
                    SamordningYtelseVurderingGrunnlagDTO(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        ytelser = registerYtelser.map { ytelse ->
                            SamordningYtelseDTO(
                                ytelseType = ytelse.ytelseType,
                                periode = Periode(fom = ytelse.periode.fom, tom = ytelse.periode.tom),
                                gradering = ytelse.gradering?.prosentverdi(),
                                kronesum = ytelse.kronesum?.toInt(),
                                kilde = ytelse.kilde,
                                saksRef = ytelse.saksRef,
                                endringStatus = ytelse.endringStatus
                            )
                        },
                        vurderinger = samordning?.vurderinger.orEmpty().flatMap { vurdering ->
                            vurdering.vurderingPerioder.map {
                                SamordningVurderingDTO(
                                    ytelseType = vurdering.ytelseType,
                                    gradering = it.gradering?.prosentverdi(),
                                    periode = Periode(fom = it.periode.fom, tom = it.periode.tom),
                                    kronesum = it.kronesum?.toInt(),
                                    manuell = it.manuell,
                                )
                            }
                        },
                        begrunnelse = samordning?.begrunnelse,
                        maksDato = samordning?.maksDato,
                        maksDatoEndelig = samordning?.maksDatoEndelig,
                        tpYtelser = tp
                    )
                )

            }
        }


            route("/{referanse}/grunnlag/samordning-andre-statlige-ytelser") {
                authorizedGet<BehandlingReferanse, SamordningAndreStatligeYtelserGrunnlagDTO>(
                    AuthorizationParamPathConfig(
                        behandlingPathParam = BehandlingPathParam(
                            "referanse"
                        )
                    )
                )
                { behandlingReferanse ->
                    val samordningAndreStatligeYtelserVurdering = dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val samordningAndreStatligeYtelserRepository =
                            repositoryProvider.provide<SamordningAndreStatligeYtelserRepository>()
                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

                        val behandling = behandlingRepository.hent(behandlingReferanse)

                        samordningAndreStatligeYtelserRepository.hentHvisEksisterer(behandling.id)?.vurdering
                    }

                val tilgangGateway = GatewayProvider.provide(TilgangGateway::class)
                val harTilgangTilÅSaksbehandle = tilgangGateway.sjekkTilgangTilBehandling(
                    behandlingReferanse.referanse,
                    Definisjon.SAMORDNING_ANDRE_STATLIGE_YTELSER.kode.toString(),
                    token()
                )


                respond(
                    SamordningAndreStatligeYtelserGrunnlagDTO(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        vurdering = SamordningAndreStatligeYtelserVurderingDTO(
                            begrunnelse = samordningAndreStatligeYtelserVurdering?.begrunnelse ?: "",
                            vurderingPerioder = (samordningAndreStatligeYtelserVurdering?.vurderingPerioder
                                ?: listOf<SamordningAndreStatligeYtelserVurderingPeriode>()).map {
                                SamordningAndreStatligeYtelserVurderingPeriodeDTO(
                                    periode = it.periode,
                                    ytelse = it.ytelse,
                                    beløp = it.beløp
                                )
                            }
                        )
                    )
                )
            }
        }
    }
}

private fun mapSamordningUføreVurdering(vurdering: SamordningUføreVurdering?): SamordningUføreVurderingDTO? {
    return vurdering?.let {
        SamordningUføreVurderingDTO(
            begrunnelse = it.begrunnelse,
            vurderingPerioder = it.vurderingPerioder.map { periode ->
                SamordningUføreVurderingPeriodeDTO(
                    periode.virkningstidspunkt,
                    periode.uføregradTilSamordning.prosentverdi()
                )
            })
    }
}

private fun mapSamordningUføreGrunnlag(registerGrunnlagVurderinger: List<UførePeriodeMedEndringStatus>): List<SamordningUføreGrunnlagDTO> {
    return registerGrunnlagVurderinger.map {
        SamordningUføreGrunnlagDTO(
            virkningstidspunkt = it.virkningstidspunkt,
            uføregrad = it.uføregrad.prosentverdi(),
            kilde = it.kilde,
            endringStatus = it.endringStatus
        )
    }
}