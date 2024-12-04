package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.medlemskap.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import javax.sql.DataSource

fun NormalOpenAPIRoute.medlemskapsgrunnlagApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/medlemskap") {
            get<BehandlingReferanse, MedlemskapGrunnlagDto> { req ->
                val medlemskap = dataSource.transaction(readOnly = true) {
                    val behandling = BehandlingReferanseService(BehandlingRepositoryImpl(it)).behandling(req)
                    MedlemskapRepository(it).hentHvisEksisterer(behandling.id)
                        ?: MedlemskapUnntakGrunnlag(unntak = listOf())
                }
                respond(MedlemskapGrunnlagDto(medlemskap = medlemskap))
            }
        }
    }
}
