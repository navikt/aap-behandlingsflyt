package no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid

import no.nav.aap.behandlingsflyt.behandling.vilkår.Varighetsvurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.mapMedDatoTilDatoVarighet
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype

class OvergangArbeidVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<OvergangArbeidFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGARBEIDVILKÅRET)

    override fun vurder(grunnlag: OvergangArbeidFaktagrunnlag) {
        vilkår.leggTilVurderinger(
            grunnlag.overgangArbeidGrunnlag
                .gjeldendeVurderinger(maksDato = grunnlag.rettighetsperiode.tom)
                .mapMedDatoTilDatoVarighet(
                    harBegrensetVarighet = { it.brukerRettPåAAP },
                    varighet = {
                        /* Vilkåret har en begrensning på maks 6 måneder. Eksempel på 6-måneders-periode
                         * fra regelspesifiseringen: 01.02.23 - 31.07.23 */
                        it.plusMonths(6).minusDays(1)
                    },
                ) { varighetsvurdering, vurdering ->
                    when (varighetsvurdering) {
                        Varighetsvurdering.VARIGHET_OK ->
                            if (vurdering.brukerRettPåAAP)
                                Vilkårsvurdering(
                                    utfall = Utfall.OPPFYLT,
                                    begrunnelse = vurdering.begrunnelse,
                                    faktagrunnlag = grunnlag,
                                    manuellVurdering = true
                                )
                            else
                                Vilkårsvurdering(
                                    utfall = Utfall.IKKE_OPPFYLT,
                                    avslagsårsak = Avslagsårsak.IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER,
                                    begrunnelse = vurdering.begrunnelse,
                                    faktagrunnlag = grunnlag,
                                    manuellVurdering = true
                                )

                        Varighetsvurdering.VARIGHET_OVERSKREDET ->
                            Vilkårsvurdering(
                                utfall = Utfall.IKKE_OPPFYLT,
                                avslagsårsak = Avslagsårsak.VARIGHET_OVERSKREDET_ARBEIDSSØKER,
                                begrunnelse = "Varighet overskredet.",
                                faktagrunnlag = grunnlag,
                                manuellVurdering = false
                            )
                    }
                }
        )
    }
}
