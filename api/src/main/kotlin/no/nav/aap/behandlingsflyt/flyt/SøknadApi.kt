package no.nav.aap.behandlingsflyt.flyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.EMPTY_JSON_RESPONSE
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.repository.RepositoryProvider
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.MDC
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.søknadApi(dataSource: DataSource) {
    route("/api/soknad") {
        route("/send").post<Unit, String, SøknadSendDto> { _, dto ->
            MDC.putCloseable("saksnummer", dto.saksnummer).use {
                dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val sakRepository = repositoryProvider.provide(SakRepository::class)
                    val sakService = SakService(sakRepository)

                    val sak = sakService.hent(Saksnummer(dto.saksnummer))

                    val flytJobbRepository = FlytJobbRepository(connection)
                    val dokumentReferanse = InnsendingReferanse(JournalpostId(dto.journalpostId))
                    flytJobbRepository.leggTil(
                        HendelseMottattHåndteringJobbUtfører.nyJobb(
                            sakId = sak.id,
                            dokumentReferanse = dokumentReferanse,
                            brevkategori = InnsendingType.SØKNAD,
                            // TODO få kanal fra payload i stedet
                            kanal = Kanal.DIGITAL,
                            periode = Periode(
                                LocalDate.now(),
                                LocalDate.now().plusYears(3)
                            ), // TODO: Sette innsendingsdato
                            payload = dto.søknad,
                        )
                    )
                }
            }
            // Må ha String-respons på grunn av Accept-header. Denne må returnere json
            respond(EMPTY_JSON_RESPONSE, HttpStatusCode.Accepted)
        }
    }
}
