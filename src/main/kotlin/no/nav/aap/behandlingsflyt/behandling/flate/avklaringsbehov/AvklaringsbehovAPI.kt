package no.nav.aap.behandlingsflyt.behandling.flate.avklaringsbehov

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import no.nav.aap.behandlingsflyt.flyt.ValiderBehandlingTilstand
import no.nav.aap.behandlingsflyt.hendelse.mottak.HendelsesMottak
import no.nav.aap.behandlingsflyt.hendelse.mottak.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.behandlingsflyt.prosessering.TaSkriveLåsRepository
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklaringsbehovApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/løs-behov") {
            post<Unit, LøsAvklaringsbehovPåBehandling, LøsAvklaringsbehovPåBehandling> { _, request ->
                dataSource.transaction { connection ->
                    val taSkriveLåsRepository = TaSkriveLåsRepository(connection)
                    val lås = taSkriveLåsRepository.lås(request.referanse)
                    val behandling = BehandlingRepository(connection).hent(lås.behandlingSkrivelås.id)

                    ValiderBehandlingTilstand.validerTilstandBehandling(
                        behandling = behandling,
                        avklaringsbehov = listOf(request.behov.definisjon()),
                        versjon = request.behandlingVersjon
                    )

                    HendelsesMottak(dataSource).håndtere(
                        key = behandling.id,
                        hendelse = LøsAvklaringsbehovBehandlingHendelse(request.behov)
                    )
                    taSkriveLåsRepository.verifiserSkrivelås(lås)
                }
                respond(request)
            }
        }
    }
}