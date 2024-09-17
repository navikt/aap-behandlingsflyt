package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.server.prosessering.BREVKODE
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringOppgaveUtfører
import no.nav.aap.behandlingsflyt.server.prosessering.JOURNALPOST_ID
import no.nav.aap.behandlingsflyt.server.prosessering.MOTTATT_TIDSPUNKT
import no.nav.aap.behandlingsflyt.server.prosessering.PERIODE
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.aktivitetspliktApi(dataSource: DataSource) {
    route("/api/aktivitetsplikt") {
        route("/lagre").post<Unit, String, BruddAktivitetspliktRequest> { _, req ->
            dataSource.transaction { connection ->

                val sakService = SakService(connection)
                val sak = sakService.hent(Saksnummer(req.saksnummer))
                val innsendingsId = UUID.randomUUID()

                val repository = BruddAktivitetspliktRepository(connection)
                repository.lagreBruddAktivitetspliktHendelse(req)

                val tom = req.perioder.maxOf { periode -> periode.tom }
                val fom = req.perioder.minOf { periode -> periode.fom }

                val flytJobbRepository = FlytJobbRepository(connection)
                flytJobbRepository.leggTil(
                    JobbInput(HendelseMottattHåndteringOppgaveUtfører)
                        .forSak(sak.id.toLong())
                        .medCallId()
                        .medParameter(JOURNALPOST_ID, innsendingsId.toString())
                        .medParameter(BREVKODE, Brevkode.AKTIVITETSKORT.name)
                        .medParameter(PERIODE, DefaultJsonMapper.toJson(Periode(fom, tom)))
                        .medParameter(MOTTATT_TIDSPUNKT, DefaultJsonMapper.toJson(LocalDateTime.now()))
                        .medPayload(DefaultJsonMapper.toJson(innsendingsId))
                )
            }
            respond("{}", HttpStatusCode.Accepted)
        }

        route("/{saksnummer}").get<HentHendelseDto, BruddAktivitetspliktResponse> { req ->
            val response = dataSource.transaction { connection ->
                val repository = BruddAktivitetspliktRepository(connection)
                val alleBrudd = repository.hentBruddAktivitetspliktHendelser(req.saksnummer)
                BruddAktivitetspliktResponse(alleBrudd)
            }
            respond(response)
        }

        route("/slett").post<Unit, String, String> { _, req ->
            dataSource.transaction { connection ->
                val repository = BruddAktivitetspliktRepository(connection)
                repository.deleteAll()
            }
            respond("{}", HttpStatusCode.Accepted)
        }
    }
}