package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.InnsendingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.aktivitet.TorsHammerDto
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Kanal
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.HentSakDTO
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import javax.sql.DataSource

fun NormalOpenAPIRoute.torsHammerApi(dataSource: DataSource) {
    route("/api/hammer") {
        route("/send").post<Unit, String, TorsHammerDto> { _, dto ->
            dataSource.transaction { connection ->
                val sakService = SakService(SakRepositoryImpl(connection))

                val sak = sakService.hent(Saksnummer(dto.saksnummer))

                val flytJobbRepository = FlytJobbRepository(connection)
                flytJobbRepository.leggTil(
                    HendelseMottattHåndteringJobbUtfører.nyJobb(
                        sakId = sak.id,
                        dokumentReferanse = MottattDokumentReferanse(InnsendingId.ny()),
                        brevkode = Brevkode.AKTIVITETSKORT,
                        kanal = Kanal.DIGITAL,
                        periode = Periode(dto.hammer.dato, dto.hammer.dato),
                        payload = dto
                    )
                )
            }
            respond("{}", HttpStatusCode.Accepted)
        }
        route("/{saksnummer}").get<HentSakDTO, AlleHammereDto> { dto ->
            val response = dataSource.transaction(readOnly = true) { connection ->
                val sakService = SakService(SakRepositoryImpl(connection))
                val sak = sakService.hent(Saksnummer(dto.saksnummer))

                val mottattDokumentRepository = MottattDokumentRepository(connection)

                val hentDokumenterAvType =
                    mottattDokumentRepository.hentDokumenterAvType(sak.id, Brevkode.AKTIVITETSKORT)


                AlleHammereDto(hentDokumenterAvType.mapNotNull { it.strukturerteData<TorsHammerDto>()?.data?.hammer })
            }
            respond(response)

        }
    }
}
