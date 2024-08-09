package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.auth.token
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.ManuelleBarnService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.ManuellebarnVurderingRepository
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
                val dto = dataSource.transaction { connection ->
                    val behandling: Behandling =
                        BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                    val manuelleBarnVurderingRepository = ManuellebarnVurderingRepository(connection)
                    val barnRepository = BarnRepository(connection)
                    val manuelleBarnService = ManuelleBarnService(manuelleBarnVurderingRepository, barnRepository, PdlPersoninfoGateway)

                    manuelleBarnService.samleManuelleBarnGrunnlag(behandling.id, token())
                }
                respond(dto)
            }
        }
    }
}