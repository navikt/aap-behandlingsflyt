package no.nav.aap.behandlingsflyt.hendelse

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.EMPTY_JSON_RESPONSE
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("hendelse.MottattHendelseAPI")

fun NormalOpenAPIRoute.mottattHendelseApi(dataSource: DataSource) {
    route("/api/hendelse") {
        // TODO legg på authorization. Her tillates både maskin-maskin og folk?
        route("/send").post<Unit, String, Innsending>(TagModule(listOf(Tags.MottaHendelse))) { _, dto ->
            MDC.putCloseable("saksnummer", dto.saksnummer.toString()).use {
                dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val sak = repositoryProvider.provide<SakRepository>().hent(dto.saksnummer)

                    logger.info("Mottok dokumenthendelse. Brevkategori: ${dto.type}.")

                    val flytJobbRepository = FlytJobbRepository(connection)
                    flytJobbRepository.leggTil(
                        HendelseMottattHåndteringJobbUtfører.nyJobb(
                            sakId = sak.id,
                            dokumentReferanse = dto.referanse,
                            brevkategori = dto.type,
                            kanal = dto.kanal,
                            melding = dto.melding,
                        ),
                    )

                }
            }
            respond(EMPTY_JSON_RESPONSE, HttpStatusCode.Accepted)
        }
    }
}