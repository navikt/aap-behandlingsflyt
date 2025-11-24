package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.tilkjentYtelseAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/tilkjentV2/{referanse}") {
            authorizedGet<BehandlingReferanse, TilkjentYtelse2Dto>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
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
                                .segmenter()
                                .map {
                                    VurdertPeriode(
                                        periode = it.periode,
                                        felter = Felter(
                                            dagsats = it.verdi.dagsats.verdi.toDouble(),
                                            barneTilleggsats = it.verdi.barnetilleggsats.verdi.toDouble(),
                                            barnetillegg = it.verdi.barnetillegg.verdi().toDouble(),
                                            arbeidGradering = 100.minus(
                                                it.verdi.graderingGrunnlag.arbeidGradering.prosentverdi()
                                            ),
                                            samordningGradering = it.verdi.graderingGrunnlag.samordningGradering.prosentverdi()
                                                .plus(it.verdi.graderingGrunnlag.samordningUføregradering.prosentverdi()),
                                            institusjonGradering = it.verdi.graderingGrunnlag.institusjonGradering.prosentverdi(),
                                            arbeidsgiverGradering = it.verdi.graderingGrunnlag.samordningArbeidsgiverGradering.prosentverdi(),
                                            totalReduksjon = 100.minus(it.verdi.gradering.prosentverdi()),
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