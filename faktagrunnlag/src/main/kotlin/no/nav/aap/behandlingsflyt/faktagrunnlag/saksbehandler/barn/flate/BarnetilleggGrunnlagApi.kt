package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.auth.token
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.ManuellebarnVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoGateway
import no.nav.aap.verdityper.sakogbehandling.Ident
import javax.sql.DataSource

fun NormalOpenAPIRoute.barnetilleggApi(dataSource: DataSource) {
    route("/api/barnetillegg") {
        route("/grunnlag/{referanse}") {
            get<BehandlingReferanse, BarnetilleggGrunnlagDto> { req ->
                val token = token()
                val dto = dataSource.transaction { connection ->
                    val behandling: Behandling =
                        BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                    val manueltOppgitteBarn = emptyList<Barn>() // TODO bruk repositroy for manuelle barn
                    val manuelleBarnVurdering = ManuellebarnVurderingRepository(connection)
                        .hentHvisEksisterer(behandling.id)?.vurdering

                    val barnDto = manueltOppgitteBarn.map { barn ->
                        val barnPersoninfo =
                            PdlPersoninfoGateway.hentPersoninfoForIdent(barn.ident, token)
                        IdentifiserteBarnDto(
                            navn = barnPersoninfo.fultNavn(),
                            ident = barnPersoninfo.ident,
                        )
                    }

                    BarnetilleggGrunnlagDto(
                        listOf( // TODO ikke bruk hardkodede verdier
                            IdentifiserteBarnDto("Pelle Potet", Ident("12345678912")),
                            IdentifiserteBarnDto("Kåre Kålrabi", Ident("12121212121"))
                        ),
                        ManuelleBarnVurderingDto.fromManuelleBarnVurdering(manuelleBarnVurdering)
                    )
                }
                respond(dto)
            }
        }
    }
}