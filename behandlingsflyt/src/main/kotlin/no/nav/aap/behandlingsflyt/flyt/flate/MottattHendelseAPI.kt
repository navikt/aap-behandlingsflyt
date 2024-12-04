package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.MottattHendelseDto
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.MDC
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.mottattHendelseApi(dataSource: DataSource) {
    route("/api/hendelse") {
        route("/send").post<Unit, String, MottattHendelseDto> { _, dto ->
            MDC.putCloseable("saksnummer", dto.saksnummer.toString()).use {
                dataSource.transaction { connection ->
                    val sakService = SakService(SakRepositoryImpl(connection))
                    val sak = sakService.hent(dto.saksnummer)

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
                            payload = dto.payload ?: dto
                        )
                    )
                }
            }
            respond("{}", HttpStatusCode.Accepted)
        }
    }
}