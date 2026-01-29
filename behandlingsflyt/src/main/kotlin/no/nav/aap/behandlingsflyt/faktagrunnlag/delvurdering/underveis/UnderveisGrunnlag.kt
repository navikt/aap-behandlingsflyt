package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class UnderveisGrunnlag(
    val id: Long,
    val perioder: List<Underveisperiode>
) {
    fun somTidslinje(): Tidslinje<Underveisperiode> {
        return perioder.somTidslinje { it.periode }
    }

    fun sisteDagMedYtelse() = perioder.last { it.utfall == Utfall.OPPFYLT }.periode.tom

    fun utledInnfriddePerioderForRettighet(rettighetsType: RettighetsType): List<Underveisperiode> {
        return perioder.filter { it.rettighetsType == rettighetsType && it.avslagsårsak == null }
    }

    fun utledStartdatoForRettighet(rettighetsType: RettighetsType): LocalDate? {
        return utledInnfriddePerioderForRettighet(rettighetsType).firstOrNull()?.periode?.fom
    }

    fun utledMaksdatoForRettighet(type: RettighetsType): LocalDate? {
        val gjenværendeKvote = utledKvoterForRettighetstype(type).gjenværendeKvote

        if (gjenværendeKvote == 0) {
            return utledInnfriddePerioderForRettighet(type).last().periode.tom
        }
        return Hverdager(utledKvoterForRettighetstype(type).gjenværendeKvote).fraOgMed(LocalDate.now())
    }

    fun utledKvoterForRettighetstype(rettighetsType: RettighetsType): RettighetKvoter {
        val bruktKvote = utledInnfriddePerioderForRettighet(rettighetsType)
            .somTidslinje { it.periode }.begrensetTil(Periode(Tid.MIN, LocalDate.now())).segmenter()
            .sumOf { it.periode.antallHverdager().asInt }
        val totalKvote = KvoteService().beregn().hentKvoteForRettighetstype(rettighetsType)?.asInt
        val gjenværendeKvote = totalKvote?.minus(bruktKvote) ?: 0

        return RettighetKvoter(
            totalKvote = totalKvote,
            bruktKvote = bruktKvote,
            gjenværendeKvote = gjenværendeKvote
        )
    }
}
