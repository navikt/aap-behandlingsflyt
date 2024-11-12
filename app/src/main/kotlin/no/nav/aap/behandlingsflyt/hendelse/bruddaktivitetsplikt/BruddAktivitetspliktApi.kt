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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Kanal
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import javax.sql.DataSource

fun NormalOpenAPIRoute.aktivitetspliktApi(dataSource: DataSource) {
    route("/api/aktivitetsplikt") {
        route("{saksnummer}") {
            route("/opprett").post<SaksnummerParameter, String, OpprettAktivitetspliktDTO> { params, req ->
                val navIdent = bruker()
                dataSource.transaction { connection ->
                    opprettDokument(connection, navIdent, Saksnummer(params.saksnummer), req)
                }
                respond("{}", HttpStatusCode.Accepted)
            }

            route("/oppdater").post<SaksnummerParameter, String, OppdaterAktivitetspliktDTO> { params, req ->
                val navIdent = bruker()
                dataSource.transaction { connection ->
                    opprettDokument(connection, navIdent, Saksnummer(params.saksnummer), req)
                }
                respond("{}", HttpStatusCode.Accepted)
            }

            route("/feilregistrer").post<SaksnummerParameter, String, FeilregistrerAktivitetspliktDTO> { params, req ->
                val navIdent = bruker()
                dataSource.transaction { connection ->
                    opprettDokument(connection, navIdent, Saksnummer(params.saksnummer), req)
                }
                respond("{}", HttpStatusCode.Accepted)
            }

            get<SaksnummerParameter, BruddAktivitetspliktResponse> { params ->
                val response = dataSource.transaction { connection ->
                    val repository = AktivitetspliktRepository(connection)
                    val sak = SakService(connection).hent(Saksnummer(params.saksnummer))
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

private fun opprettDokument(connection: DBConnection, navIdent: Bruker, saksnummer: Saksnummer, req: AktivitetspliktDTO) {
    val aktivitetspliktServce = AktivitetspliktService(
        repository = AktivitetspliktRepository(connection)
    )

    val sak = SakService(connection).hent(saksnummer)

    val aktivitetspliktDokumenter = req.tilDomene(sak.id, navIdent)
    val innsendingId = aktivitetspliktServce.registrerBrudd(aktivitetspliktDokumenter)

    registrerDokumentjobb(innsendingId, aktivitetspliktDokumenter, connection, sak)
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
            kanal = Kanal.DIGITAL,
            dokumentReferanse = dokumentReferanse,
            periode = Periode(fom, tom),
            payload = innsendingId
        )
    )
}
