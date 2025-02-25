package no.nav.aap.behandlingsflyt.datadeling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import javax.sql.DataSource



fun NormalOpenAPIRoute.datadelingAPI(datasource: DataSource) {
    route("/api/datadeling") {
        route("/perioder") {
            post<Unit, List<Maksimum.Periode>, InternVedtakRequest> { request, body ->
                val person = datasource.transaction { conn ->
                    val repositoryProvider = RepositoryProvider(conn)
                    val personRepository = repositoryProvider.provide<PersonRepository>()
                    personRepository.finn(Ident(body.personidentifikator))
                }
                if (person != null) {
                    val saker = datasource.transaction { conn ->
                        val repositoryProvider = RepositoryProvider(conn)
                        val sakRepository = repositoryProvider.provide<SakRepository>()
                        sakRepository.finnSakerFor(person)
                    }
                    respond(saker.map { sak ->
                        Maksimum.Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
                    })
                } else {
                    respond(emptyList())
                }
            }
            route("/aktivitetsfase") {
                post<Unit, List<PeriodeMedAktFaseKode>, InternVedtakRequest> { request, body ->
                    val person = datasource.transaction { conn ->
                        val repositoryProvider = RepositoryProvider(conn)
                        val personRepository = repositoryProvider.provide<PersonRepository>()
                        personRepository.finn(Ident(body.personidentifikator))
                    }
                    if (person != null) {
                        val saker = datasource.transaction { conn ->
                            val repositoryProvider = RepositoryProvider(conn)
                            val sakRepository = repositoryProvider.provide<SakRepository>()
                            sakRepository.finnSakerFor(person)
                        }
                        respond(saker.map { sak ->
                            PeriodeMedAktFaseKode(
                                aktivitetsfaseKode = "", // Snakk med fredrik - rettighetsType
                                aktivitetsfaseNavn = "",
                                periode = Maksimum.Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
                            )
                        })
                    } else {
                        respond(emptyList())
                    }
                }
            }
        }
        route("/sakerByFnr") {
            post<Unit, List<SakStatus>, SakerRequest> { request, body ->
                val person = datasource.transaction { conn ->
                    val repositoryProvider = RepositoryProvider(conn)
                    val personRepository = repositoryProvider.provide<PersonRepository>()
                    personRepository.finn(Ident(body.personidentifikatorer.first()))
                }
                if (person != null) {
                    val saker = datasource.transaction { conn ->
                        val repositoryProvider = RepositoryProvider(conn)
                        val sakRepository = repositoryProvider.provide<SakRepository>()
                        sakRepository.finnSakerFor(person)
                    }
                    respond(saker.map { sak ->
                        SakStatus.fromKelvin(
                            sak.id.id.toString(),
                            sak.status(),
                            Maksimum.Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.tom),
                        )
                    })
                } else {
                    respond(emptyList())
                }

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
                        vedtaksId = "", // TODO: hvor finner jeg denne?
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
                        utbetaling = listOf( // Regner oss frem til utbetaling
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

fun selectLastBehandling(datasource: DataSource, fnr: String):  Behandling? {
    return datasource.transaction(readOnly = true) { conn ->
        val repositoryProvider = RepositoryProvider(conn)
        val personRepository = repositoryProvider.provide<PersonRepository>()
        val sakRepository = repositoryProvider.provide<SakRepository>()
        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

        val person = personRepository.finn(Ident(fnr)) ?: return@transaction null
        val sak = sakRepository.finnSakerFor(person)

        /* TODO: her ønsker vi vel siste avsluttede behandling? */
        behandlingRepository.finnSisteBehandlingFor(sak.last().id)
    }
}