package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.MottattHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Kanal
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import org.slf4j.MDC
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.mottattHendelseApi(dataSource: DataSource) {
    route("/api/hendelse") {
        route("/send").post<Unit, String, MottattHendelseDto> { _, dto ->
            MDC.putCloseable("saksnummer", dto.saksnummer).use {
                dataSource.transaction { connection ->
                    val sakService = SakService(SakRepositoryImpl(connection))
                    val sak = sakService.hent(Saksnummer(dto.saksnummer))

                    val flytJobbRepository = FlytJobbRepository(connection)
                    val dokumentReferanse = mapDokumentReferanse(dto)

                    flytJobbRepository.leggTil(
                        HendelseMottattHåndteringJobbUtfører.nyJobb(
                            sakId = sak.id,
                            dokumentReferanse = dokumentReferanse,
                            brevkode = dto.type,
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

private fun mapDokumentReferanse(dto: MottattHendelseDto): MottattDokumentReferanse {
    return when (dto.type) {
        Brevkode.LEGEERKLÆRING_MOTTATT -> MottattDokumentReferanse(MottattDokumentReferanse.Type.JOURNALPOST, dto.hendelseId.toString() )
        Brevkode.LEGEERKLÆRING_AVVIST ->MottattDokumentReferanse(MottattDokumentReferanse.Type.AVVIST_LEGEERKLÆRING_ID, dto.hendelseId.toString() )
        Brevkode.DIALOGMELDING -> MottattDokumentReferanse(MottattDokumentReferanse.Type.JOURNALPOST, dto.hendelseId.toString() )
        Brevkode.SØKNAD -> TODO()
        Brevkode.AKTIVITETSKORT -> TODO()
        Brevkode.PLIKTKORT -> TODO()
        Brevkode.UKJENT -> TODO()
    }
}