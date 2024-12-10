package no.nav.aap.behandlingsflyt.hendelse

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.EMPTY_JSON_RESPONSE
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.MottattHendelseDto
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.LocalDate
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("hendelse.MottattHendelseAPI")

fun NormalOpenAPIRoute.mottattHendelseApi(dataSource: DataSource) {
    route("/api/hendelse") {
        route("/send").post<Unit, String, MottattHendelseDto>(TagModule(listOf(Tags.MottaHendelse))) { _, dto ->
            MDC.putCloseable("saksnummer", dto.saksnummer.toString()).use {
                dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val sak = repositoryProvider.provide(SakRepository::class).hent(dto.saksnummer)

                    logger.info("Mottok hendelse. Brevkategori: ${dto.type}.")

                    val flytJobbRepository = FlytJobbRepository(connection)
                    flytJobbRepository.leggTil(
                        HendelseMottattHåndteringJobbUtfører.nyJobb(
                            sakId = sak.id,
                            dokumentReferanse = dto.hendelseId,
                            brevkategori = dto.type,
                            kanal = Kanal.DIGITAL,
                            periode = Periode(
                                LocalDate.now(),
                                LocalDate.now().plusWeeks(4)
                            ),
                            payload = dto.payload
                        )
                    )
                }
            }
            respond(EMPTY_JSON_RESPONSE, HttpStatusCode.Accepted)
        }
    }
}