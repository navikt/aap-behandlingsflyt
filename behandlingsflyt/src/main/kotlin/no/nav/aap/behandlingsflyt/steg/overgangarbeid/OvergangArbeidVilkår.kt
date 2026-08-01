package no.nav.aap.behandlingsflyt.steg.overgangarbeid

import java.time.LocalDate
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.misc.Varighetsvurdering
import no.nav.aap.misc.Vilkårsvurderer
import no.nav.aap.misc.mapMedDatoTilDatoVarighet
import no.nav.aap.overgangarbeid.OvergangArbeidFaktagrunnlag
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkår
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.vilkårsresultat.Vilkårtype

class OvergangArbeidVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<OvergangArbeidFaktagrunnlag> {
    companion object {
        fun utledVarighetSluttdato(fraDato: LocalDate): LocalDate = fraDato.plusMonths(6).minusDays(1)
    }

    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGARBEIDVILKÅRET)

    // TODO: Bør det ikke være konsistenssjekk med sykdom og bistand her?
    override fun vurder(grunnlag: OvergangArbeidFaktagrunnlag) {
        vilkår.leggTilVurderinger(
            grunnlag.overgangArbeidGrunnlag
                .gjeldendeVurderinger()
                .begrensetTil(Periode(Tid.MIN, grunnlag.rettighetsperiode.tom))
                .mapMedDatoTilDatoVarighet(
                    harBegrensetVarighet = { it.brukerRettPåAAP },
                    varighet = {
                        /* Vilkåret har en begrensning på maks 6 måneder. Eksempel på 6-måneders-periode
                         * fra regelspesifiseringen: 01.02.23 - 31.07.23 */
                        utledVarighetSluttdato(it)
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