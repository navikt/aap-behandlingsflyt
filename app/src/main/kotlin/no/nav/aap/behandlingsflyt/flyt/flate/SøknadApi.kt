package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.server.prosessering.BREVKODE
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringOppgaveUtfører
import no.nav.aap.behandlingsflyt.server.prosessering.JOURNALPOST_ID
import no.nav.aap.behandlingsflyt.server.prosessering.MOTTATT_TIDSPUNKT
import no.nav.aap.behandlingsflyt.server.prosessering.PERIODE
import no.nav.aap.behandlingsflyt.server.prosessering.MOTTATT_DOKUMENT_REFERANSE
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.dokument.JournalpostId
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
                val dokumentReferanse = MottattDokumentReferanse(JournalpostId(dto.journalpostId))
                flytJobbRepository.leggTil(
                    JobbInput(HendelseMottattHåndteringOppgaveUtfører)
                        .forSak(sak.id.toLong())
                        .medCallId()
                        .medParameter(JOURNALPOST_ID, "")
                        .medParameter(MOTTATT_DOKUMENT_REFERANSE, DefaultJsonMapper.toJson(dokumentReferanse))
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
            respond("{}", HttpStatusCode.Accepted)
        }
    }
}
