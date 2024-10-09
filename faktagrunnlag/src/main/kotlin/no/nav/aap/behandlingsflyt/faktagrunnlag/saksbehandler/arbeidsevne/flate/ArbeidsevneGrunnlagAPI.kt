package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.arbeidsevneGrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/arbeidsevne") {
        get<BehandlingReferanse, ArbeidsevneGrunnlagDto> { req ->
            val arbeidsevneGrunnlag = dataSource.transaction { connection ->
                val behandling: Behandling =
                    BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                ArbeidsevneRepository(connection).hentHvisEksisterer(behandling.id)
            }
            arbeidsevneGrunnlag?.let { respond(it.toDto()) } ?: respondWithStatus(HttpStatusCode.NoContent)
        }
    }
}