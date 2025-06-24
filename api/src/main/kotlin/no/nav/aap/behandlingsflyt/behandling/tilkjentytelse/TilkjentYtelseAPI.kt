package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
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
                                    grunnlag = it.grunnlag.verdi,
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

                    val tilkjentYtelse = TilkjentYtelseService(
                        behandlingRepository,
                        tilkjentYtelseRepository
                    ).hentTilkjentYtelse(req);

                        tilkjentYtelse.groupBy { it.aktuellMeldeperiode }.map { (meldeperiode, vurdertePerioder) ->
                            TilkjentYtelsePeriode2Dto(
                                meldeperiode = meldeperiode,
                                levertMeldekortDato = LocalDate.now(),
                                meldekortStatus = MeldekortStaus.IKKE_LEVERT,
                                vurdertePerioder = vurdertePerioder.map { it ->
                                    VurdertPeriode(
                                        fraOgMed = it.periode.fom,
                                        tilOgMed = it.periode.tom,
                                        dagsats = it.tilkjent.dagsats.verdi,
                                        barneTilleggsats = it.tilkjent.barnetilleggsats.verdi,
                                        arbeidGradering = it.tilkjent.gradering.arbeidGradering?.prosentverdi(),
                                        samordningGradering = it.tilkjent.gradering.samordningGradering?.prosentverdi(),
                                        institusjonGradering = it.tilkjent.gradering.institusjonGradering?.prosentverdi(),
                                        totalReduksjon = 27,
                                        effektivDagsats = 150
                                    )
                                }
                            )
                        }


                }
                respond(TilkjentYtelse2Dto(perioder = tilkjentYtelsePerioder))
            }
        }

    }
}