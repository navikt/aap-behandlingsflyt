package no.nav.aap.behandlingsflyt.avklaringsbehov.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import org.slf4j.MDC
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklaringsbehovApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/løs-behov") {
            post<Unit, LøsAvklaringsbehovPåBehandling, LøsAvklaringsbehovPåBehandling> { _, request ->
                dataSource.transaction { connection ->
                    val taSkriveLåsRepository = TaSkriveLåsRepository(connection)
                    val lås = taSkriveLåsRepository.lås(request.referanse)

                    MDC.putCloseable("sakId", lås.sakSkrivelås.id.toString()).use {
                        MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString()).use {

                            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                                key = lås.behandlingSkrivelås.id,
                                hendelse = LøsAvklaringsbehovBehandlingHendelse(
                                    request.behov,
                                    request.ingenEndringIGruppe,
                                    request.behandlingVersjon
                                )
                            )
                            taSkriveLåsRepository.verifiserSkrivelås(lås)
                        }
                    }
                }
                respond(request)
            }
        }
    }
}