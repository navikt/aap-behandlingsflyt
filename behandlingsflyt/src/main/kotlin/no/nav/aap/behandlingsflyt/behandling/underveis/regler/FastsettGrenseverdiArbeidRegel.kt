package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Prosent

// § 11-23 fjerde ledd
private const val HØYESTE_GRADERING_NORMAL = 60

// § 11-23 fjerde ledd
private const val HØYESTE_GRADERING_YRKESSKADE = 70

// § 11-23 sjette ledd
private const val HØYESTE_GRADERING_OPPTRAPPING = 80

class FastsettGrenseverdiArbeidRegel: UnderveisRegel {
    override fun vurder(
        input: UnderveisInput,
        resultat: Tidslinje<Vurdering>
    ): Tidslinje<Vurdering> {
        val graderingsgrenseverdier = graderingsgrenseverdier(input, resultat)
        return resultat.leggTilVurderinger(graderingsgrenseverdier, Vurdering::leggTilGrenseverdi)
    }

    private fun graderingsgrenseverdier(
        input: UnderveisInput,
        resultat: Tidslinje<Vurdering>
    ): Tidslinje<Prosent> {
        val opptrappingTidslinje =
            Tidslinje(input.opptrappingPerioder.map { Segment(it, Prosent(HØYESTE_GRADERING_OPPTRAPPING)) })

        val harYrkesskade = input.vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).vilkårsperioder()
            .any { it.utfall == Utfall.OPPFYLT && it.innvilgelsesårsak == Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG }

        val øvreGrenseNormalt = if (harYrkesskade) {
            Prosent(HØYESTE_GRADERING_YRKESSKADE)
        } else {
            Prosent(HØYESTE_GRADERING_NORMAL)
        }
        return resultat.mapValue { øvreGrenseNormalt }
            .kombiner(opptrappingTidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
    }
}