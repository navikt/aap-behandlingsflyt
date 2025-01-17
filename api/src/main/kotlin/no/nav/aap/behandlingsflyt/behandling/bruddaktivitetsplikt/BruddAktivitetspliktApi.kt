package no.nav.aap.behandlingsflyt.behandling.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.DokumentInput
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AktivitetskortV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.Kanal
import javax.sql.DataSource

fun NormalOpenAPIRoute.aktivitetspliktApi(dataSource: DataSource) {
    route("/api").tag(Tags.Aktivitetsplikt) {
        route("/behandling/{referanse}/aktivitetsplikt/effektuer").get<BehandlingReferanse, Effektuer11_7Dto> { behandlingReferanse ->
            val respons = dataSource.transaction { conn ->
                val repositoryProvider = RepositoryProvider(conn)
                val underveisRepository = repositoryProvider.provide(UnderveisRepository::class)
                val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                val aktivitetspliktRepository = repositoryProvider.provide(AktivitetspliktRepository::class)
                val effektuer117Repository = repositoryProvider.provide(Effektuer11_7Repository::class)

                val behandlingId = BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse).id

                val brudd = underveisRepository.hent(behandlingId)
                    .perioder
                    .mapNotNull {
                        val id = it.bruddAktivitetspliktId ?: return@mapNotNull null
                        Segment(it.periode, id)
                    }
                    .let { Tidslinje(it) }
                    .komprimer()
                    .map { segment ->
                        val dokument = aktivitetspliktRepository.hentBrudd(segment.verdi)
                        bruddAktivitetspliktHendelseDto(dokument, segment.periode)
                    }

                val effektuer11_7Grunnlag = effektuer117Repository.hentHvisEksisterer(behandlingId)
                val sisteVarsel = effektuer11_7Grunnlag?.varslinger?.maxByOrNull { it.datoVarslet }

                Effektuer11_7Dto(
                    begrunnelse = effektuer11_7Grunnlag?.vurdering?.begrunnelse,
                    forhåndsvarselDato = sisteVarsel?.datoVarslet,
                    forhåndsvarselSvar = null,
                    gjeldendeBrudd = brudd,
                )
            }

            respond(respons)
        }

        route("/sak/{saksnummer}/aktivitetsplikt") {
            route("/opprett").post<SaksnummerParameter, String, OpprettAktivitetspliktDTO> { params, req ->
                val navIdent = bruker()
                dataSource.transaction { connection ->
                    opprettDokument(connection, navIdent, Saksnummer(params.saksnummer), req)
                }
                respond("{}", HttpStatusCode.Accepted)
            }

            route("/oppdater").post<SaksnummerParameter, String, OppdaterAktivitetspliktDTOV2> { params, req ->
                val navIdent = bruker()
                dataSource.transaction { connection ->
                    opprettDokument(connection, navIdent, Saksnummer(params.saksnummer), req)
                }
                respond("{}", HttpStatusCode.Accepted)
            }

            get<SaksnummerParameter, BruddAktivitetspliktResponse> { params ->
                val response = dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val sakRepository = repositoryProvider.provide(SakRepository::class)
                    val aktivitetspliktRepository = repositoryProvider.provide(AktivitetspliktRepository::class)

                    val sak = SakService(sakRepository).hent(Saksnummer(params.saksnummer))
                    val alleBrudd = aktivitetspliktRepository.hentBrudd(sak.id).utledBruddTilstand()
                    BruddAktivitetspliktResponse(alleBrudd)
                }
                respond(response)
            }
        }

    }
}

private fun opprettDokument(
    connection: DBConnection,
    navIdent: Bruker,
    saksnummer: Saksnummer,
    req: AktivitetspliktDTO
) {
    val repositoryProvider = RepositoryProvider(connection)
    val sakRepository = repositoryProvider.provide(SakRepository::class)
    val repository = repositoryProvider.provide(AktivitetspliktRepository::class)

    val sak = SakService(sakRepository).hent(saksnummer)

    val aktivitetspliktDokumenter = req.tilDomene(sak, navIdent)
    val innsendingId = repository.lagreBrudd(sak.id, aktivitetspliktDokumenter)

    registrerDokumentjobb(innsendingId, aktivitetspliktDokumenter, connection, sak)
}

private fun registrerDokumentjobb(
    innsendingId: InnsendingId,
    bruddAktivitetsplikt: List<DokumentInput>,
    connection: DBConnection,
    sak: Sak
) {
    val dokumentReferanse = InnsendingReferanse(innsendingId)

    val tom = bruddAktivitetsplikt.maxOf { dokument -> dokument.brudd.periode.tom }
    val fom = bruddAktivitetsplikt.minOf { dokument -> dokument.brudd.periode.fom }

    val flytJobbRepository = FlytJobbRepository(connection)
    flytJobbRepository.leggTil(
        HendelseMottattHåndteringJobbUtfører.nyJobb(
            sakId = sak.id,
            brevkategori = InnsendingType.AKTIVITETSKORT,
            kanal = Kanal.DIGITAL,
            dokumentReferanse = dokumentReferanse,
            melding = AktivitetskortV0(fraOgMed = fom, tilOgMed = tom),
        )
    )
}
