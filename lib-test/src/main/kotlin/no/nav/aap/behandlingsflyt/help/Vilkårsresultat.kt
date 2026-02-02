package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.type.Periode

fun genererVilkårsresultat(
    periode: Periode,
    aldersVilkåret: Vilkår = Vilkår(
        Vilkårtype.ALDERSVILKÅRET, setOf(
            Vilkårsperiode(
                periode,
                Utfall.OPPFYLT,
                false,
                null,
                faktagrunnlag = null
            )
        )
    ),
    sykdomsVilkåret: Vilkår =
        Vilkår(
            Vilkårtype.SYKDOMSVILKÅRET, setOf(
                Vilkårsperiode(
                    periode,
                    Utfall.OPPFYLT,
                    false,
                    null,
                    faktagrunnlag = null
                )
            )
        ),
    lovvalgsVilkåret: Vilkår =
        Vilkår(
            Vilkårtype.LOVVALG, setOf(
                Vilkårsperiode(
                    periode,
                    Utfall.OPPFYLT,
                    false,
                    null,
                    faktagrunnlag = null
                )
            )
        ),
    medlemskapVilkåret: Vilkår =
        Vilkår(
            Vilkårtype.MEDLEMSKAP, setOf(
                Vilkårsperiode(
                    periode,
                    Utfall.OPPFYLT,
                    false,
                    null,
                    faktagrunnlag = null
                )
            )
        ),
    bistandVilkåret: Vilkår =
        Vilkår(
            Vilkårtype.BISTANDSVILKÅRET, setOf(
                Vilkårsperiode(
                    periode,
                    Utfall.OPPFYLT,
                    false,
                    null,
                    faktagrunnlag = null,
                    avslagsårsak = null
                )
            )
        ),
    grunnlagVilkåret: Vilkår = Vilkår(
        Vilkårtype.GRUNNLAGET, setOf(
            Vilkårsperiode(
                periode,
                Utfall.OPPFYLT,
                false,
                null,
                faktagrunnlag = null
            )
        )
    )
): Vilkårsresultat {
    return Vilkårsresultat(
        vilkår = listOf(
            aldersVilkåret,
            lovvalgsVilkåret,
            sykdomsVilkåret,
            medlemskapVilkåret,
            bistandVilkåret,
            grunnlagVilkåret,
        )
    )
}