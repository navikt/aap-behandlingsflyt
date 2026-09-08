package no.nav.aap.behandlingsflyt.hendelse.datadeling

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp

/**
 * Hvilket barn (identifisert med personnummer der det finnes) som gir rett til
 * barnetillegg, og i hvilke perioder, med det gradert (faktiske) beløpet barnet
 * bidrar med i hver periode.
 */
data class BarnMedBarnetillegg(
    /** Personnummer til barnet. `null` hvis barnet kun er registrert med navn/fødselsdato. */
    val ident: String?,
    val perioderMedBarnetillegg: List<PeriodeMedBeløp>,
)

data class PeriodeMedBeløp(val periode: Periode, val beløp: Beløp)

/**
 * Kombinerer tilkjent ytelse-perioder (som har gradering og barnetilleggsats) med
 * barnetillegg-grunnlaget (som har hvilke barn som gir rett til barnetillegg i hvilke
 * perioder).
 */
fun utledBarnMedBarnetillegg(
    tilkjentYtelse: List<TilkjentYtelsePeriode>,
    barnetilleggGrunnlag: BarnetilleggGrunnlag?,
): List<BarnMedBarnetillegg> {
    val tilkjentTidslinje = tilkjentYtelse.tilTidslinje()
    val rettTidslinje = barnetilleggGrunnlag?.perioder.tilTidslinje()

    val barnPerioder: List<Pair<BarnIdentifikator, PeriodeMedBeløp>> = tilkjentTidslinje
        .innerJoin(rettTidslinje) { periode, tilkjent, rett ->
            rett.barnMedRettTil().map { barn ->
                barn to PeriodeMedBeløp(periode, tilkjent.barnetilleggsats.multiplisert(tilkjent.gradering))
            }
        }
        .segmenter()
        .flatMap { it.verdi }

    return barnPerioder
        .groupBy({ it.first }, { it.second })
        .map { (barn, perioder) ->
            BarnMedBarnetillegg(
                ident = when (barn) {
                    is BarnIdentifikator.BarnIdent -> barn.ident.identifikator
                    is BarnIdentifikator.NavnOgFødselsdato -> null
                },
                perioderMedBarnetillegg = perioder.sortedBy { it.periode.fom },
            )
        }
}
