package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.AvklaringsType
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningGradering
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.samordning.YtelseGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere.slåSammenTilListe
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import kotlin.math.min

data class SamordningYtelseVurderingGrunnlag(
    val ytelseGrunnlag: SamordningYtelseGrunnlag?,
    val vurderingGrunnlag: SamordningVurderingGrunnlag?,
): Faktagrunnlag {
    fun tilTidslinje(): Tidslinje<SamordningGradering> {
        val manuelleVurderinger = vurderingGrunnlag?.tilTidslinje().orEmpty()

        /**
         * Henter kun automatiske ytelser fra register - disse skal ikke ha overlappende perioder
         * Pr nå har vi ingen typer som er satt opp til å vurderes automatisk
         */
        val hentedeYtelserFraRegisterForAutomatiskVurdering =
            ytelseGrunnlag?.ytelser.orEmpty()
                .filter { it.ytelseType.type == AvklaringsType.AUTOMATISK }
                .map { ytelse ->
                    Tidslinje(ytelse.ytelsePerioder.map { Segment(it.periode, Pair(ytelse.ytelseType, it)) })
                }.fold(Tidslinje.empty<List<Pair<Ytelse, SamordningYtelsePeriode>>>()) { acc, curr ->
                    acc.kombiner(curr, slåSammenTilListe())
                }

        // Slå sammen med vurderinger og regn ut graderinger
        return hentedeYtelserFraRegisterForAutomatiskVurdering.kombiner(
            manuelleVurderinger,
            JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
                // Manuelt vurderte perioder er allerede validert
                val manueltVurderteGraderinger =
                    høyre?.verdi.orEmpty().associate { it.first to it.second }
                        .mapValues { it.value.gradering!! }
                        .filterKeys { it.type == AvklaringsType.MANUELL }

                val registerVurderinger =
                    venstre?.verdi.orEmpty().associate { it.first to it.second.gradering!! }
                        .filterKeys { it.type == AvklaringsType.AUTOMATISK }

                val alleSammen = manueltVurderteGraderinger.plus(registerVurderinger)
                val gradering = min(alleSammen.values.sumOf { it.prosentverdi() }, 100)
                Segment(
                    periode, SamordningGradering(
                        gradering = Prosent(gradering),
                        ytelsesGraderinger = alleSammen.entries.map { YtelseGradering(it.key, it.value) }
                    )
                )
            }
        )
    }

    fun perioderSomIkkeHarBlittVurdert(): Tidslinje<List<Ytelse>> {
        val ytelseTidslinje = tidslinjeMedSamordningYtelser()
        val vurderingTidslinje = vurderingGrunnlag?.tilTidslinje().orEmpty()
        return ytelseTidslinje.kombiner(vurderingTidslinje, StandardSammenslåere.minus())
    }

    fun tidslinjeMedSamordningYtelser(): Tidslinje<List<Ytelse>> {
        return ytelseGrunnlag?.ytelser.orEmpty()
            .filter { it.ytelseType.type == AvklaringsType.MANUELL }
            .map { ytelse ->
                val tidslinjePerPeriode = ytelse.ytelsePerioder.map { Tidslinje(it.periode, ytelse.ytelseType) }
                tidslinjePerPeriode.fold(Tidslinje.empty<Ytelse>()) { acc, curr ->
                    acc.kombiner(curr, StandardSammenslåere.prioriterHøyreSideCrossJoin())
                }.komprimer()
            }.fold(Tidslinje.empty()) { acc, curr ->
                acc.kombiner(curr, slåSammenTilListe())
            }
    }
}

/**
 * Grunnlag fra samordningssteget som brukes i følgende steg.
 *
 * Alle fakta ligger i [SamordningYtelseVurderingGrunnlag] og
 * lagres ned som [no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag] sammen med denne.
 */
data class SamordningGrunnlag(
    val samordningPerioder: Set<SamordningPeriode>,
)

/**
 * En ferdig vurdert samordning-periode.
 */
data class SamordningPeriode(
    val periode: Periode,
    val gradering: Prosent
)
