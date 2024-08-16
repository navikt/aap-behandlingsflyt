package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.medlemskap.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.tilgang.Ressurs
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.RessursType
import no.nav.aap.tilgang.authorizedGet

fun NormalOpenAPIRoute.medlemskapsgrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route(
            "/{referanse}/grunnlag/medlemskap",
        ) {
            authorizedGet<BehandlingReferanse, MedlemskapGrunnlagDto>(
                Operasjon.SE,
                Ressurs("referanse", RessursType.Behandling)
            ) { req ->
                val medlemskap = dataSource.transaction(block = hentMedlemsskap(req))
                respond(MedlemskapGrunnlagDto(medlemskap = medlemskap))
            }
        }
    }
}

private fun hentMedlemsskap(req: BehandlingReferanse): (DBConnection) -> MedlemskapUnntakGrunnlag =
    {
        val behandling = BehandlingReferanseService(BehandlingRepositoryImpl(it)).behandling(req)
        MedlemskapService.konstruer(it).hentHvisEksisterer(behandling.id) ?: MedlemskapUnntakGrunnlag(unntak = listOf())
    }