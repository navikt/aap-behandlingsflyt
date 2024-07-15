package no.nav.aap.behandlingsflyt.behandling.barnetillegg.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.auth.token
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoGateway
import javax.sql.DataSource

fun NormalOpenAPIRoute.barnetilleggApi(dataSource: DataSource) {
    route("/api/barnetillegg") {
        route("/grunnlag/{referanse}") {
            get<BehandlingReferanse, BarnetilleggDto> { req ->
                val token = token()
                val dto = dataSource.transaction { connection ->
                    val behandling: Behandling =
                        BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                    val folkeregisterBarn = BarnRepository(connection).hent(behandling.id)

                    val barnDto = folkeregisterBarn.barn.map { barn ->
                        val barnPersoninfo =
                            PdlPersoninfoGateway.hentPersoninfoForIdent(barn.ident, token)
                        IdentifiserteBarnDto(
                            navn = barnPersoninfo.fultNavn(),
                            ident = barnPersoninfo.ident,
                            forsorgerPeriode = barn.periodeMedRettTil()
                        )
                    }

                    BarnetilleggDto(folkeregisterbarn = barnDto)
                }

                respond(dto)
            }
        }
    }
}