package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Dokumenttype.BRUDD
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.server.authenticate.innloggetNavIdent
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import javax.sql.DataSource

fun NormalOpenAPIRoute.aktivitetspliktApi(dataSource: DataSource) {
    route("/api/aktivitetsplikt") {
        route("/lagre").post<Unit, String, BruddAktivitetspliktRequest> { _, req ->
            dataSource.transaction { connection ->
                val navIdent = innloggetNavIdent()
                val sak = SakService(connection).hent(Saksnummer(req.saksnummer))

                val repository = AktivitetspliktRepository(connection)
                val bruddAktivitetsplikt = req.perioder.map { periode ->
                    AktivitetspliktRepository.DokumentInput(
                        sakId = sak.id,
                        brudd = req.brudd,
                        paragraf = req.paragraf,
                        begrunnelse = req.begrunnelse,
                        periode = periode,
                        innsender = navIdent,
                        dokumenttype = BRUDD,
                        grunn = req.grunn ?: BruddAktivitetsplikt.Grunn.INGEN_GYLDIG_GRUNN
                    )
                }
                val innsendingId = repository.lagreBrudd(bruddAktivitetsplikt)
                val dokumentReferanse = MottattDokumentReferanse(innsendingId)

                val tom = req.perioder.maxOf { periode -> periode.tom }
                val fom = req.perioder.minOf { periode -> periode.fom }

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
            respond("{}", HttpStatusCode.Accepted)
        }

        route("/{saksnummer}").get<HentHendelseDto, BruddAktivitetspliktResponse> { req ->
            val response = dataSource.transaction { connection ->
                val repository = AktivitetspliktRepository(connection)
                val sak = SakService(connection).hent(Saksnummer(req.saksnummer))
                val alleBrudd = repository.hentBrudd(sak.id)
                    .map { dokument ->
                        BruddAktivitetspliktHendelseDto(
                            brudd = dokument.brudd,
                            paragraf = dokument.paragraf,
                            periode = dokument.periode,
                            begrunnelse = dokument.begrunnelse,
                            hendelseId = dokument.hendelseId.toString(),
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