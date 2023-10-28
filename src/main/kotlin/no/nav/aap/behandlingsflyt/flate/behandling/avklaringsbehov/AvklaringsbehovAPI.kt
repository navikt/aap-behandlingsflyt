package no.nav.aap.behandlingsflyt.flate.behandling.avklaringsbehov

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.throws
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import no.nav.aap.behandlingsflyt.flyt.ValiderBehandlingTilstand
import no.nav.aap.behandlingsflyt.hendelse.mottak.HendelsesMottak
import no.nav.aap.behandlingsflyt.hendelse.mottak.LøsAvklaringsbehovBehandlingHendelse
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklaringsbehovApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/løs-behov").throws(HttpStatusCode.BadRequest, IllegalArgumentException::class) {
            post<Unit, LøsAvklaringsbehovPåBehandling, LøsAvklaringsbehovPåBehandling> { _, request ->
                dataSource.transaction {
                    val behandling = BehandlingRepository(it).hent(request.referanse)

                    ValiderBehandlingTilstand.validerTilstandBehandling(
                        behandling,
                        listOf(request.behov.definisjon())
                    )

                    HendelsesMottak(dataSource).håndtere(
                        key = behandling.id,
                        hendelse = LøsAvklaringsbehovBehandlingHendelse(request.behov, request.behandlingVersjon)
                    )
                }
                respond(request)
            }
        }
    }
}