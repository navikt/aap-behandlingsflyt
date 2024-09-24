package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktRepository
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.server.authenticate.innloggetNavIdent
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringOppgaveUtfører
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

                val repository = BruddAktivitetspliktRepository(connection)
                val bruddAktivitetsplikt = req.perioder.map { periode ->
                    BruddAktivitetspliktRepository.LagreBruddInput(
                        sakId = sak.id,
                        brudd = req.brudd,
                        paragraf = req.paragraf,
                        begrunnelse = req.begrunnelse,
                        periode = periode,
                        navIdent = navIdent,
                    )
                }
                val innsendingId = repository.lagreBrudd(bruddAktivitetsplikt)
                val dokumentReferanse = MottattDokumentReferanse(innsendingId)

                val tom = req.perioder.maxOf { periode -> periode.tom }
                val fom = req.perioder.minOf { periode -> periode.fom }

                val flytJobbRepository = FlytJobbRepository(connection)
                flytJobbRepository.leggTil(
                    HendelseMottattHåndteringOppgaveUtfører.nyJobb(
                        sakId = sak.id,
                        brevkode = Brevkode.AKTIVITETSKORT,
                        dokumentReferanse = dokumentReferanse,
                        periode = Periode(fom, tom),
                    )
                )
            }
            respond("{}", HttpStatusCode.Accepted)
        }

        route("/{saksnummer}").get<HentHendelseDto, BruddAktivitetspliktResponse> { req ->
            val response = dataSource.transaction { connection ->
                val repository = BruddAktivitetspliktRepository(connection)
                val sak = SakService(connection).hent(Saksnummer(req.saksnummer))
                val alleBrudd = repository.hentBrudd(sak.id)
                    .map {
                        BruddAktivitetspliktHendelseDto(
                            brudd = it.brudd,
                            paragraf = it.paragraf,
                            periode = it.periode,
                            begrunnelse = it.begrunnelse,
                            hendelseId = it.hendelseId.toString(),
                        )
                    }
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