package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.bruker
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
                                BehandlingTilstandValidator(connection).validerTilstand(
                                    BehandlingReferanse(request.referanse), request.behandlingVersjon
                                )

                            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                                key = lås.behandlingSkrivelås.id, hendelse = LøsAvklaringsbehovHendelse(
                                    request.behov,
                                    request.ingenEndringIGruppe ?: false,
                                    request.behandlingVersjon,
                                    bruker()
                                )
                            )
                            taSkriveLåsRepository.verifiserSkrivelås(lås)
                        }
                    }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}