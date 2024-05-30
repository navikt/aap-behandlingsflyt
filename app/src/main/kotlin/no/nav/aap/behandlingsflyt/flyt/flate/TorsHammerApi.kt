package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.hendelse.mottak.AktivitetsmeldingMottattSakHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.HendelsesMottak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.server.respondWithStatus
import java.time.LocalDateTime
import javax.sql.DataSource

fun NormalOpenAPIRoute.torsHammerApi(dataSource: DataSource) {
    route("/api/hammer") {
        route("/send").post<Unit, String, TorsHammerDto> { _, dto ->
            HendelsesMottak(dataSource).håndtere(
                key = Saksnummer(dto.saksnummer),
                hendelse = AktivitetsmeldingMottattSakHendelse(
                    mottattTidspunkt = LocalDateTime.now(), //FIXME: Faktisk søknadstidspunkt
                    hammer = dto.hammer                )
            )
            // Må ha String-respons på grunn av Accept-header. Denne må returnere json
            respondWithStatus(HttpStatusCode.Accepted, "{}")
        }
    }
}
