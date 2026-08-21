package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.AvklaringsType
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningGradering
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.samordning.YtelseGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.outerJoin
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import kotlin.math.min

data class SamordningYtelseVurderingGrunnlag(
    val ytelseGrunnlag: SamordningYtelseGrunnlag?,
    val vurderingGrunnlag: SamordningVurderingGrunnlag?
) : Faktagrunnlag {

    fun perioderSomIkkeHarBlittVurdert(
        rettighetsperiode: Periode,
    ): Tidslinje<List<Ytelse>> {
        val hentedeYtelserByManuelleYtelser = ytelseGrunnlag?.tidslinjeMedSamordningYtelser().orEmpty()

        val perioderSomIkkeHarBlittVurdert =
            hentedeYtelserByManuelleYtelser.kombiner(
                vurderingGrunnlag?.vurderingTidslinje().orEmpty(),
                StandardSammenslåere.minus()
            ).begrensetTil(rettighetsperiode)

        return perioderSomIkkeHarBlittVurdert.komprimer()
    }

    /**
     * Regn ut samordning-gradering ved å summere grad for ytelser det skal samordnes mot.
     */
    fun vurder(): Tidslinje<SamordningGradering> {
        /**
         * Henter kun automatiske ytelser fra register - disse skal ikke ha overlappende perioder
         * Pr nå har vi ingen typer som er satt opp til å vurderes automatisk
         */
        val hentedeYtelserFraRegisterForAutomatiskVurdering =
            ytelseGrunnlag?.ytelser.orEmpty().filter { it.ytelseType.type == AvklaringsType.AUTOMATISK }.map { ytelse ->
                ytelse.ytelsePerioder.somTidslinje({it.periode}, { Pair(ytelse.ytelseType, it) })
            }.outerJoin()

        // Slå sammen med vurderinger og regn ut graderinger

        val samordningTidslinje =
            hentedeYtelserFraRegisterForAutomatiskVurdering.kombiner(
                vurderingGrunnlag?.vurderingTidslinje().orEmpty(),
                JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
                    // Manuelt vurderte perioder er allerede validert
                    val manueltVurderteGraderinger =
                        høyre?.verdi.orEmpty().associate { it.first to it.second }
                            .mapValues { it.value.gradering!! }
                            .filterKeys { it.type == AvklaringsType.MANUELL }

                    val registerVurderinger = venstre?.verdi.orEmpty().associate { it.first to it.second.gradering!! }
                        .filterKeys { it.type == AvklaringsType.AUTOMATISK }

                    val alleSammen = manueltVurderteGraderinger.plus(registerVurderinger)
                    val gradering =
                        min(alleSammen.values.sumOf { it.prosentverdi() }, 100)
                    Segment(
                        periode, SamordningGradering(
                            gradering = Prosent(gradering),
                            ytelsesGraderinger = alleSammen.entries.map { YtelseGradering(it.key, it.value) }
                        )
                    )
                })

        return samordningTidslinje
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
