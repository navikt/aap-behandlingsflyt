package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.hendelse.mottak.DokumentMottattSakHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.HendelsesMottak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

fun NormalOpenAPIRoute.søknadApi(dataSource: DataSource) {
    route("/api/soknad") {
        route("/send").post<Unit, String, SøknadSendDto> { _, dto ->
            HendelsesMottak(dataSource).håndtere(
                key = Saksnummer(dto.saksnummer),
                hendelse = DokumentMottattSakHendelse(
                    journalpost = JournalpostId(dto.journalpostId),
                    mottattTidspunkt = LocalDateTime.now(), //FIXME: Faktisk søknadstidspunkt
                    strukturertDokument = StrukturertDokument(
                        data = Søknad(
                            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
                            student = dto.søknad.student.erStudent()
                        ),
                        brevkode = Brevkode.SØKNAD
                    )
                )
            )
            //TODO: Må ha String-respons på grunn av Accept-header. Denne må returnere json
            respond("{}")
        }
    }
}
