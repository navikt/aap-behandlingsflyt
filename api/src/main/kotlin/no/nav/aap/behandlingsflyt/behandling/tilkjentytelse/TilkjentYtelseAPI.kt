package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode as TilkjentYtelsePeriodeD

fun NormalOpenAPIRoute.tilkjentYtelseAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/tilkjent/{referanse}") {
            authorizedGet<BehandlingReferanse, TilkjentYtelseDto>(
                AuthorizationParamPathConfig(
                    operasjon = Operasjon.SE,
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val tilkjentYtelser = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryFactory = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryFactory.provide<BehandlingRepository>()
                    val tilkjentYtelseRepository: TilkjentYtelseRepository =
                        repositoryFactory.provide<TilkjentYtelseRepository>()

                    TilkjentYtelseService(
                        behandlingRepository,
                        tilkjentYtelseRepository
                    ).hentTilkjentYtelse(req)
                        .map { tilkjentYtelsePeriode: TilkjentYtelsePeriodeD ->
                            tilkjentYtelsePeriode.tilkjent.let {
                                TilkjentYtelsePeriodeDTO(
                                    fraOgMed = tilkjentYtelsePeriode.periode.fom,
                                    tilOgMed = tilkjentYtelsePeriode.periode.tom,
                                    dagsats = it.dagsats.verdi,
                                    gradering = it.gradering.endeligGradering.prosentverdi(),
                                    grunnlagsfaktor = it.grunnlagsfaktor.verdi(),
                                    grunnbeløp = it.grunnbeløp.verdi,
                                    antallBarn = it.antallBarn,
                                    barnetilleggsats = it.barnetilleggsats.verdi,
                                    barnetillegg = it.barnetillegg.verdi,
                                    utbetalingsdato = it.utbetalingsdato,
                                    redusertDagsats = it.redusertDagsats().verdi().toDouble(),
                                    arbeidGradering = it.gradering.arbeidGradering?.prosentverdi(),
                                    institusjonGradering = it.gradering.institusjonGradering?.prosentverdi(),
                                    samordningGradering = it.gradering.samordningGradering?.prosentverdi(),
                                    samordningUføreGradering = it.gradering.samordningUføregradering?.prosentverdi()
                                )
                            }
                        }
                }
                respond(TilkjentYtelseDto(perioder = tilkjentYtelser))
            }
        }

        route("/tilkjentV2/{referanse}") {
            authorizedGet<BehandlingReferanse, TilkjentYtelse2Dto>(
                AuthorizationParamPathConfig(
                    operasjon = Operasjon.SE,
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val tilkjentYtelsePerioder = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryFactory = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryFactory.provide<BehandlingRepository>()
                    val tilkjentYtelseRepository: TilkjentYtelseRepository =
                        repositoryFactory.provide<TilkjentYtelseRepository>()
                    val meldekortRepository = repositoryFactory.provide<MeldekortRepository>()
                    val meldeperiodeRepository = repositoryFactory.provide<MeldeperiodeRepository>()

                    val behandling = behandlingRepository.hent(req)
                    val meldeperioder = meldeperiodeRepository.hent(behandling.id)
                    val meldekortGrunnlag = meldekortRepository.hentHvisEksisterer(behandling.id)

                    val tilkjentYtelse = TilkjentYtelseService(
                        behandlingRepository,
                        tilkjentYtelseRepository
                    ).hentTilkjentYtelse(req)

                    // Disse er sortert stigende via Meldekort::meldekort
                    val meldekortene = meldekortGrunnlag?.meldekort().orEmpty()

                    val tilkjentYtelseTidslinje = tilkjentYtelse.tilTidslinje()

                    meldeperioder.map { meldeperiode ->
                        val begrensetTil = tilkjentYtelseTidslinje.begrensetTil(meldeperiode)

                        val førsteAktuelleMeldekort =
                            meldekortene.firstOrNull { arbeidIPeriode ->
                                arbeidIPeriode.timerArbeidPerPeriode.any {
                                    it.periode.overlapper(
                                        meldeperiode
                                    )
                                }
                            }

                        val sisteAktuelleMeldekort = meldekortene.lastOrNull { meldekort ->
                            meldekort.timerArbeidPerPeriode.any {
                                it.periode.overlapper(
                                    meldeperiode
                                )
                            }
                        }

                        TilkjentYtelsePeriode2Dto(
                            meldeperiode = meldeperiode,
                            levertMeldekortDato = førsteAktuelleMeldekort?.mottattTidspunkt?.toLocalDate(),
                            sisteLeverteMeldekort = sisteAktuelleMeldekort?.let { meldekort ->
                                MeldekortDto(
                                    timerArbeidPerPeriode = ArbeidIPeriodeDto(meldekort.timerArbeidPerPeriode.sumOf {
                                        it.timerArbeid.antallTimer.toDouble()
                                    }),
                                    mottattTidspunkt = meldekort.mottattTidspunkt,
                                )
                            },
                            meldekortStatus = null,
                            vurdertePerioder = begrensetTil
                                .map {
                                    VurdertPeriode(
                                        periode = it.periode,
                                        felter = Felter(
                                            dagsats = it.verdi.dagsats.verdi.toDouble(),
                                            barneTilleggsats = it.verdi.barnetilleggsats.verdi.toDouble(),
                                            arbeidGradering = 100.minus(
                                                it.verdi.gradering.arbeidGradering?.prosentverdi() ?: 0
                                            ),
                                            samordningGradering = it.verdi.gradering.samordningGradering?.prosentverdi()
                                                ?.plus(
                                                    it.verdi.gradering.samordningUføregradering?.prosentverdi()
                                                        ?: 0
                                                ),
                                            institusjonGradering = it.verdi.gradering.institusjonGradering?.prosentverdi(),
                                            arbeidsgiverGradering = it.verdi.gradering.samordningArbeidsgiverGradering?.prosentverdi(),
                                            totalReduksjon = 100.minus(it.verdi.gradering.endeligGradering.prosentverdi()),
                                            effektivDagsats = it.verdi.redusertDagsats().verdi().toDouble()
                                        )
                                    )
                                }
                                .komprimerLikeFelter())
                    }
                }
                respond(TilkjentYtelse2Dto(perioder = tilkjentYtelsePerioder))
            }
        }

    }
}