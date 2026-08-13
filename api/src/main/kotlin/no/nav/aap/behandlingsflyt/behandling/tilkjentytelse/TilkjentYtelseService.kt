package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.utils.diff.somDto
import no.nav.aap.behandlingsflyt.utils.diffTidslinjer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import javax.sql.DataSource

class TilkjentYtelseService(
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry
) {
    fun hentTilkjentYtelse(behandlingReferanse: BehandlingReferanse): TilkjentYtelse2Dto {
        val behandling = hentBehandling(behandlingReferanse)
        return hentTilkjentYtelseForBehandling(behandling.id)
    }


    fun hentTilkjentYtelseMedDiff(behandlingReferanse: BehandlingReferanse): TilkjentYtelse2MedDiffDto {
        val behandling = hentBehandling(behandlingReferanse)
        val gjeldendeTilkjentYtelse = hentTilkjentYtelseForBehandling(behandling.id)
        val forrigeTilkjentYtelse = behandling.forrigeBehandlingId?.let { hentTilkjentYtelseForBehandling(it) }

        val diff = diffTidslinjer(
            forrige = forrigeTilkjentYtelse?.tilTidslinje() ?: Tidslinje(),
            nå = gjeldendeTilkjentYtelse.tilTidslinje()
        ).mapValue { it.somDto() }

        return TilkjentYtelse2MedDiffDto( diff.verdier().toList())
    }

    private fun hentTilkjentYtelseForBehandling(behandlingId: BehandlingId): TilkjentYtelse2Dto {
        return dataSource.transaction(readOnly = true) { connection ->
            val repositoryFactory = repositoryRegistry.provider(connection)
            val tilkjentYtelseRepository: TilkjentYtelseRepository =
                repositoryFactory.provide<TilkjentYtelseRepository>()
            val meldekortRepository = repositoryFactory.provide<MeldekortRepository>()
            val meldeperiodeRepository = repositoryFactory.provide<MeldeperiodeRepository>()
            val underveisRepository = repositoryFactory.provide<UnderveisRepository>()

            val meldekortene =
                meldekortRepository.hentHvisEksisterer(behandlingId)
                    ?.meldekort()
                    .orEmpty()

            val tilkjentYtelseTidslinje =
                tilkjentYtelseRepository.hentHvisEksisterer(behandlingId)
                    ?.tilTidslinje()
                    .orEmpty()

            val underveisTidslinje: Tidslinje<UnderveisFelter> =
                underveisRepository.hentHvisEksisterer(behandlingId)
                    ?.somTidslinje()
                    ?.mapValue {
                        UnderveisFelter(
                            andelArbeid = it.arbeidsgradering.andelArbeid,
                            grenseverdi = it.grenseverdi,
                        )
                    }
                    ?: Tidslinje()

            val meldeperioder = if (tilkjentYtelseTidslinje.isNotEmpty()) {
                meldeperiodeRepository.hentMeldeperioder(behandlingId, tilkjentYtelseTidslinje.helePerioden())
            } else {
                emptyList()
            }

            TilkjentYtelse2Dto(
                perioder = mapTilkjentYtelsePerioder(
                    meldeperioder,
                    tilkjentYtelseTidslinje,
                    underveisTidslinje,
                    meldekortene,
                )
            )
        }

    }

    private fun hentBehandling(behandlingReferanse: BehandlingReferanse): Behandling {
        val behandling = dataSource.transaction(readOnly = true) { connection ->
            val repositoryFactory = repositoryRegistry.provider(connection)
            val behandlingRepository = repositoryFactory.provide<BehandlingRepository>()
            behandlingRepository.hent(behandlingReferanse)
        }
        return behandling
    }

    private fun mapTilkjentYtelsePerioder(
        meldeperioder: List<Periode>,
        tilkjentYtelseTidslinje: Tidslinje<Tilkjent>,
        underveisTidslinje: Tidslinje<UnderveisFelter>,
        meldekortene: List<Meldekort>,
    ): List<TilkjentYtelsePeriode2Dto> = meldeperioder.map { meldeperiode ->
        val begrensetTil = tilkjentYtelseTidslinje
            .begrensetTil(meldeperiode)
            .leftJoin(underveisTidslinje, { tilkjent, underveisFelter ->
                TilkjentMedUnderveisFelter(tilkjent, underveisFelter)
            })

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
                    val tilkjent = it.verdi.tilkjent
                    val underveis = it.verdi.underveis
                    VurdertPeriode(
                        periode = it.periode,
                        felter = Felter(
                            dagsats = tilkjent.dagsats.verdi.toDouble(),
                            barneTilleggsats = tilkjent.barnetilleggsats.verdi.toDouble(),
                            barnetillegg = tilkjent.barnetillegg.verdi().toDouble(),
                            barnepensjonDagsats = tilkjent.barnepensjonDagsats.verdi().toDouble(),
                            arbeidGradering = 100.minus(
                                tilkjent.graderingGrunnlag.arbeidGradering.prosentverdi()
                            ),
                            andelArbeid = underveis?.andelArbeid?.prosentverdi(),
                            grenseverdi = underveis?.grenseverdi?.prosentverdi(),
                            samordningGradering = tilkjent.graderingGrunnlag.samordningGradering.prosentverdi()
                                .plus(tilkjent.graderingGrunnlag.samordningUføregradering.prosentverdi())
                                .coerceAtMost(100),
                            institusjonGradering = tilkjent.graderingGrunnlag.institusjonGradering.prosentverdi(),
                            arbeidsgiverGradering = tilkjent.graderingGrunnlag.samordningArbeidsgiverGradering.prosentverdi(),
                            totalReduksjon = 100.minus(tilkjent.gradering.prosentverdi()),
                            effektivDagsats = tilkjent.redusertDagsats().verdi().toDouble()
                        )
                    )
                }
                .komprimerLikeFelter())
    }

}

internal data class UnderveisFelter(
    val andelArbeid: Prosent,
    val grenseverdi: Prosent,
)

internal data class TilkjentMedUnderveisFelter(
    val tilkjent: Tilkjent,
    val underveis: UnderveisFelter?,
)