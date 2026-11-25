package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class UnderveisGrunnlag(
    val id: Long,
    val perioder: List<Underveisperiode>
) {
    fun somTidslinje(): Tidslinje<Underveisperiode> {
        return perioder.somTidslinje { it.periode }
    }

    fun meldedatoForMeldeperiode(meldeperiode: Periode): LocalDate? {
        val meldedatoer = perioder.filter { it.meldePeriode == meldeperiode }.mapNotNull { it.meldedato }.toSet()
        check(meldedatoer.size <= 1) { "[Utviklerfeil] Flere meldedatoer for samme meldeperiode" }
        return meldedatoer.singleOrNull()
    }

    fun sisteDagMedYtelse() = perioder.last { it.utfall == Utfall.OPPFYLT }.periode.tom

    fun utledInnfriddePerioderForRettighet(rettighetsType: RettighetsType): List<Underveisperiode> {
        return perioder.filter { it.rettighetsType == rettighetsType }
    }

    fun utledStartdatoForRettighet(rettighetsType: RettighetsType): LocalDate? {
        return utledInnfriddePerioderForRettighet(rettighetsType).firstOrNull()?.periode?.fom
    }

    fun rettighetstyper(): Tidslinje<RettighetsType> {
        return this.somTidslinje()
            .mapNotNull { it.rettighetsType }
            .komprimer()
    }

    fun harRett(): Boolean = rettighetstyper().isNotEmpty()
}
