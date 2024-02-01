package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeRepository

fun NormalOpenAPIRoute.sykdomsgrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykdom") {
            get<BehandlingReferanse, SykdomGrunnlagDto> { req ->
                val (yrkesskadeGrunnlag, sykdomGrunnlag) = dataSource.transaction { connection ->
                    val behandling: Behandling = BehandlingReferanseService(connection).behandling(req)

                    val yrkesskadeGrunnlag =
                        YrkesskadeRepository(connection).hentHvisEksisterer(behandlingId = behandling.id)
                    val sykdomGrunnlag = SykdomRepository(connection).hentHvisEksisterer(behandlingId = behandling.id)

                    yrkesskadeGrunnlag to sykdomGrunnlag
                }

                respond(
                    SykdomGrunnlagDto(
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.map { yrkesskade ->
                                RegistrertYrkesskade(
                                    ref = yrkesskade.ref,
                                    periode = yrkesskade.periode,
                                    kilde = "Yrkesskaderegisteret"
                                )
                            } ?: emptyList(),
                        ),
                        sykdomsvurdering = sykdomGrunnlag?.sykdomsvurdering,
                        erÅrsakssammenheng = sykdomGrunnlag?.yrkesskadevurdering?.erÅrsakssammenheng
                    )
                )
            }
        }
        route("/{referanse}/grunnlag/sykdom/yrkesskade") {
            get<BehandlingReferanse, YrkesskadeGrunnlagDto> { req ->
                val (yrkesskadeGrunnlag, sykdomGrunnlag) = dataSource.transaction { connection ->
                    val behandling: Behandling = BehandlingReferanseService(connection).behandling(req)

                    val yrkesskadeGrunnlag =
                        YrkesskadeRepository(connection).hentHvisEksisterer(behandlingId = behandling.id)
                    val sykdomGrunnlag = SykdomRepository(connection).hentHvisEksisterer(behandlingId = behandling.id)

                    yrkesskadeGrunnlag to sykdomGrunnlag
                }

                respond(
                    YrkesskadeGrunnlagDto(
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.map { yrkesskade ->
                                RegistrertYrkesskade(
                                    ref = yrkesskade.ref,
                                    periode = yrkesskade.periode,
                                    kilde = "Yrkesskaderegisteret"
                                )
                            } ?: emptyList()
                        ),
                        yrkesskadevurdering = sykdomGrunnlag?.yrkesskadevurdering,
                    )
                )
            }
        }
    }
}