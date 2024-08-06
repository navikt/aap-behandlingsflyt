package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.server.prosessering.BREVKODE
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringOppgaveUtfører
import no.nav.aap.behandlingsflyt.server.prosessering.JOURNALPOST_ID
import no.nav.aap.behandlingsflyt.server.prosessering.MOTTATT_TIDSPUNKT
import no.nav.aap.behandlingsflyt.server.prosessering.PERIODE
import no.nav.aap.behandlingsflyt.server.respondWithStatus
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

fun NormalOpenAPIRoute.søknadApi(dataSource: DataSource) {
    route("/api/soknad") {
        route("/send").post<Unit, String, SøknadSendDto> { _, dto ->
            dataSource.transaction { connection ->
                val sakService = SakService(connection)

                val sak = sakService.hent(Saksnummer(dto.saksnummer))

                val flytJobbRepository = FlytJobbRepository(connection)
                flytJobbRepository.leggTil(
                    JobbInput(HendelseMottattHåndteringOppgaveUtfører)
                        .forSak(sak.id)
                        .medCallId()
                        .medParameter(JOURNALPOST_ID, dto.journalpostId)
                        .medParameter(BREVKODE, Brevkode.SØKNAD.name)
                        .medParameter(
                            PERIODE,
                            DefaultJsonMapper.toJson(Periode(LocalDate.now(), LocalDate.now().plusYears(3))) // TODO: Sette innsendingsdato
                        )
                        .medParameter(MOTTATT_TIDSPUNKT, DefaultJsonMapper.toJson(LocalDateTime.now()))
                        .medPayload(DefaultJsonMapper.toJson(dto.søknad))
                )
            }
            // Må ha String-respons på grunn av Accept-header. Denne må returnere json
            respondWithStatus(HttpStatusCode.Accepted, "{}")
        }
    }
}
