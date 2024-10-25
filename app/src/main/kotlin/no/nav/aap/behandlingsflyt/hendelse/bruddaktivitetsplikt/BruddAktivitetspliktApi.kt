package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.InnsendingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.aktiveBrudd
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.server.authenticate.innloggetNavIdent
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import javax.sql.DataSource

fun NormalOpenAPIRoute.aktivitetspliktApi(dataSource: DataSource) {
    route("/api/aktivitetsplikt") {
        route("/lagre").post<Unit, String, BruddAktivitetspliktRequest> { _, req ->
            dataSource.transaction { connection ->
                val aktivitetspliktServce = AktivitetspliktService(
                    repository = AktivitetspliktRepository(connection)
                )

                val navIdent = innloggetNavIdent()
                val sak = SakService(connection).hent(Saksnummer(req.saksnummer))


                val aktivitetspliktDokument = req.perioder.map { periode ->
                    req.tilDomene(sak.id, periode, navIdent)
                }

                val innsendingId = aktivitetspliktServce.registrerBrudd(aktivitetspliktDokument)
                registrerDokumentjobb(innsendingId, aktivitetspliktDokument, connection, sak)
            }
            respond("{}", HttpStatusCode.Accepted)
        }

        route("/{saksnummer}").get<HentHendelseDto, BruddAktivitetspliktResponse> { req ->
            val response = dataSource.transaction { connection ->
                val repository = AktivitetspliktRepository(connection)
                val sak = SakService(connection).hent(Saksnummer(req.saksnummer))
                val alleBrudd = repository.hentBrudd(sak.id)
                    .aktiveBrudd()
                    .map { dokument ->
                        BruddAktivitetspliktHendelseDto(
                            brudd = dokument.brudd.bruddType,
                            paragraf = dokument.brudd.paragraf,
                            periode = dokument.brudd.periode,
                            grunn = dokument.grunn,
                        )
                    }
                BruddAktivitetspliktResponse(alleBrudd)
            }
            respond(response)
        }

        route("/slett").post<Unit, String, String> { _, _ ->
            dataSource.transaction { connection ->
                val repository = AktivitetspliktRepository(connection)
                repository.deleteAll()
            }
            respond("{}", HttpStatusCode.Accepted)
        }
    }
}

private fun registrerDokumentjobb(
    innsendingId: InnsendingId,
    bruddAktivitetsplikt: List<AktivitetspliktRepository.DokumentInput>,
    connection: DBConnection,
    sak: Sak
) {
    val dokumentReferanse = MottattDokumentReferanse(innsendingId)

    val tom = bruddAktivitetsplikt.maxOf { dokument -> dokument.brudd.periode.tom }
    val fom = bruddAktivitetsplikt.minOf { dokument -> dokument.brudd.periode.fom }

    val flytJobbRepository = FlytJobbRepository(connection)
    flytJobbRepository.leggTil(
        HendelseMottattHåndteringJobbUtfører.nyJobb(
            sakId = sak.id,
            brevkode = Brevkode.AKTIVITETSKORT,
            dokumentReferanse = dokumentReferanse,
            periode = Periode(fom, tom),
            payload = innsendingId
        )
    )
}
