package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService

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

                val innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.map { yrkesskade ->
                    RegistrertYrkesskade(
                        ref = yrkesskade.ref,
                        periode = yrkesskade.periode,
                        kilde = "Yrkesskaderegisteret"
                    )
                } ?: emptyList()
                respond(
                    SykdomGrunnlagDto(
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = innhentedeYrkesskader,
                        ),
                        sykdomsvurdering = sykdomGrunnlag?.sykdomsvurdering?.toDto(sykdomGrunnlag.yrkesskadevurdering),
                        skalVurdereYrkesskade = innhentedeYrkesskader.isNotEmpty()
                    )
                )
            }
        }
    }
}