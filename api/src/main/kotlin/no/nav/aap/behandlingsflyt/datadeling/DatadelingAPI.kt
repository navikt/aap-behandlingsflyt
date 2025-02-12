package no.nav.aap.behandlingsflyt.datadeling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import javax.sql.DataSource

fun NormalOpenAPIRoute.datadelingAPI(datasource: DataSource) {
    route("/api/datadeling") {
        route("/sakerByFnr"){
            post<Unit, List<SakStatus>, SakerRequest>{ request, body ->
                val person = datasource.transaction(readOnly = true) { conn ->
                    val repositoryProvider = RepositoryProvider(conn)
                    val personRepository = repositoryProvider.provide<PersonRepository>()
                    personRepository.finn(Ident(body.personidentifikatorer.first()))
                }
                if (person == null) {
                    respond(emptyList())
                }
                val saker = datasource.transaction(readOnly = true) { conn ->
                    val repositoryProvider = RepositoryProvider(conn)
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    sakRepository.finnSakerFor(person!!)
                }
                respond(saker.map { sak ->
                    SakStatus(
                        sak.id.toString(),
                        SakStatus.VedtakStatus.valueOf(sak.status().toString()),
                        Maksimum.Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.tom),
                    )
                })

            }
        }
        route("/vedtak") {
            post<Unit, Maksimum, DatadelingRequest> { request, body ->
                val person = datasource.transaction(readOnly = true) { conn ->
                    val repositoryProvider = RepositoryProvider(conn)
                    val personRepository = repositoryProvider.provide<PersonRepository>()
                    personRepository.finn(Ident("12345678910"))
                }
                if (person == null) {
                    respond(Maksimum(emptyList()))
                }
                val sak = datasource.transaction(readOnly = true) { conn ->
                    val repositoryProvider = RepositoryProvider(conn)
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    sakRepository.finnSakerFor(person!!)
                }
                val behandling = datasource.transaction(readOnly = true) { conn ->
                    val repositoryProvider = RepositoryProvider(conn)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    behandlingRepository.finnSisteBehandlingFor(sak.last().id)
                }

                val tilkjentYtelse = requireNotNull(datasource.transaction(readOnly = true) { conn ->
                    val repositoryProvider = RepositoryProvider(conn)
                    val tilkjentYtelseRepository = repositoryProvider.provide<TilkjentYtelseRepository>()
                    tilkjentYtelseRepository.hentHvisEksisterer(behandling!!.id)
                })

                val beregning = requireNotNull(datasource.transaction(readOnly = true) { conn ->
                    val repositoryProvider = RepositoryProvider(conn)
                    val tilkjentYtelseRepository = repositoryProvider.provide<BeregningsgrunnlagRepository>()
                    tilkjentYtelseRepository.hentHvisEksisterer(behandling!!.id)
                })



                respond(Maksimum(tilkjentYtelse.map { segment ->
                    Maksimum.Vedtak(
                        dagsats = beregning.grunnlaget().verdi()
                            .toInt(), // TODO: Her må vi gange med dagens grunnbeløp og dele på 260
                        status = "OK", // TODO: Denne må mappes ut til noe som gir mening
                        saksnummer = sak.last().id.toString(),
                        vedtaksdato = "", // TODO: hvor finner jeg denne?
                        vedtaksTypeKode = "", // TODO: Disse må vi mappe om til noe som gir mening
                        vedtaksTypeNavn = "", // TODO: -||-
                        periode = Maksimum.Periode(segment.fom(), segment.tom()), // TODO: er dette rettighets periode?
                        rettighetsType = "", // TODO: Disse må vi mappe om til noe som gir mening
                        beregningsgrunnlag = beregning.grunnlaget().verdi()
                            .toInt(), //TODO: Dette må ganges med dagens grunnbeløp
                        barnMedStonad = segment.verdi.antallBarn, // TODO: Vi må lage en tidslinje/liste med vedtak for å kunne hente ut antall barn
                        kildesystem = "Kelvin",
                        samordningsId = null, //TODO: mangler en så lenge
                        opphorsAarsak = null, // TODO: mengler en så lenge,
                        utbetaling = listOf(
                            Maksimum.UtbetalingMedMer( //TODO: Denne må utvides når vi vet mer om hvordan utbetalinger ser ut
                                null,
                                segment.verdi.gradering.prosentverdi(), // TODO: dobbelsjekk at dette er riktig
                                Maksimum.Periode(segment.fom(), segment.tom()),
                                segment.verdi.dagsats.multiplisert(10)
                                    .pluss(segment.verdi.barnetillegg.multiplisert(10)).verdi.toInt(),
                                segment.verdi.dagsats.verdi.toInt(),
                                segment.verdi.barnetillegg.verdi.toInt()
                            )
                        )// TODO: Vi må lage en tidslinje/liste med vedtak for å kunne hente ut utbetalinger
                    )
                }))

            }

        }
    }
}

