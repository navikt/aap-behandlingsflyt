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

    fun utledMaksdatoForRettighet(type: RettighetsType, gjeldendeDato: LocalDate): LocalDate? {
        val gjenværendeKvote = utledKvoterForRettighetstype(type, gjeldendeDato).gjenværendeKvote

        if (gjenværendeKvote == 0) {
            return utledInnfriddePerioderForRettighet(type).last().periode.tom
        }
        return Hverdager(gjenværendeKvote).fraOgMed(gjeldendeDato)
    }

    fun utledKvoterForRettighetstype(rettighetsType: RettighetsType, gjeldendeDato: LocalDate): RettighetKvoter {
        val totalKvote = KvoteService().beregn().hentKvoteForRettighetstype(rettighetsType)?.asInt
        val perioderForRettighet = utledInnfriddePerioderForRettighet(rettighetsType)
        val periodeKvoter = perioderForRettighet.map {
            val bruktKvote = perioderForRettighet
                .somTidslinje { it.periode }.begrensetTil(Periode(Tid.MIN, it.periode.tom)).segmenter()
                .sumOf { it.periode.antallHverdager().asInt }

            PeriodeKvote(
                periode = it.periode,
                bruktKvote = bruktKvote,
                gjenværendeKvote = totalKvote?.minus(bruktKvote)
            )
        }
        val senesteInnfriddePeriode = periodeKvoter.lastOrNull { it.periode.fom <= gjeldendeDato }
        val periodeSluttdato = senesteInnfriddePeriode?.periode?.tom
        val ubruktKvoteIPeriode =
            if (periodeSluttdato != null && periodeSluttdato.isAfter(gjeldendeDato)) {
                Periode(
                    gjeldendeDato.plusDays(1),
                    periodeSluttdato
                ).antallHverdager().asInt
            } else 0
        val gjenværendeKvote = senesteInnfriddePeriode?.gjenværendeKvote?.plus(ubruktKvoteIPeriode) ?: totalKvote ?: 0

        return RettighetKvoter(
            totalKvote = totalKvote,
            bruktKvote = totalKvote?.minus(gjenværendeKvote) ?: 0,
            gjenværendeKvote = gjenværendeKvote,
            periodeKvoter = periodeKvoter
        )
    }
}
