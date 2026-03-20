package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import javax.sql.DataSource
import kotlin.collections.firstOrNull
import kotlin.collections.lastOrNull
import kotlin.collections.orEmpty

class TilkjentYtelseService(
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry
) {
    fun hentTilkjentYtelse(behandlingReferanse: BehandlingReferanse): TilkjentYtelse2Dto {
        return dataSource.transaction(readOnly = true) { connection ->
            val repositoryFactory = repositoryRegistry.provider(connection)
            val behandlingRepository = repositoryFactory.provide<BehandlingRepository>()
            val tilkjentYtelseRepository: TilkjentYtelseRepository =
                repositoryFactory.provide<TilkjentYtelseRepository>()
            val meldekortRepository = repositoryFactory.provide<MeldekortRepository>()
            val meldeperiodeRepository = repositoryFactory.provide<MeldeperiodeRepository>()

            val behandling = behandlingRepository.hent(behandlingReferanse)

            val meldekortene =
                meldekortRepository.hentHvisEksisterer(behandling.id)
                    ?.meldekort()
                    .orEmpty()

            val tilkjentYtelseTidslinje =
                tilkjentYtelseRepository.hentHvisEksisterer(behandling.id)
                    ?.tilTidslinje()
                    .orEmpty()

            val meldeperioder = if (tilkjentYtelseTidslinje.isNotEmpty()) {
                meldeperiodeRepository.hentMeldeperioder(behandling.id, tilkjentYtelseTidslinje.helePerioden())
            } else {
                emptyList()
            }

            TilkjentYtelse2Dto(
                perioder = mapTilkjentYtelsePerioder(
                    meldeperioder,
                    tilkjentYtelseTidslinje,
                    meldekortene
                )
            )
        }
    }

    private fun mapTilkjentYtelsePerioder(
        meldeperioder: List<Periode>,
        tilkjentYtelseTidslinje: Tidslinje<Tilkjent>,
        meldekortene: List<Meldekort>
    ): List<TilkjentYtelsePeriode2Dto> = meldeperioder.map { meldeperiode ->
        val begrensetTil = tilkjentYtelseTidslinje.begrensetTil(meldeperiode)

        val førsteAktuelleMeldekort =
            meldekortene.firstOrNull { arbeidIPeriode ->
                arbeidIPeriode.timerArbeidPerPeriode.any {
                    it.periode.overlapper(meldeperiode)
                }
            }

        val sisteAktuelleMeldekort = meldekortene.lastOrNull { meldekort ->
            meldekort.timerArbeidPerPeriode.any {
                it.periode.overlapper(meldeperiode)
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
                            barnepensjonDagsats = it.verdi.barnepensjonDagsats.verdi().toDouble(),
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