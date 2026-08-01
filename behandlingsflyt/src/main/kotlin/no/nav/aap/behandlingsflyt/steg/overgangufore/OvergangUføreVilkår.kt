package no.nav.aap.behandlingsflyt.steg.overgangufore

import java.time.LocalDate
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.misc.Varighetsvurdering
import no.nav.aap.misc.Vilkårsvurderer
import no.nav.aap.misc.mapMedDatoTilDatoVarighet
import no.nav.aap.overganguføre.OvergangUføreFaktagrunnlag
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkår
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.vilkårsresultat.Vilkårtype

class OvergangUføreVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<OvergangUføreFaktagrunnlag> {
    companion object {
        fun utledVarighetSluttdato(fraDato: LocalDate): LocalDate = fraDato.plusMonths(8).minusDays(1)
    }

    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGUFØREVILKÅRET)
    override fun vurder(grunnlag: OvergangUføreFaktagrunnlag) {
        val vurderinger =
            grunnlag.overgangUføreGrunnlag
                ?.somOvergangUforevurderingstidslinje()
                .orEmpty()
                .mapMedDatoTilDatoVarighet(
                    harBegrensetVarighet = { it.harRettPåAAPMedOvergangUføre() },
                    varighet = {
                        /* Fra lovteksten § 11-18:
                                 * > Det kan gis arbeidsavklaringspenger i inntil åtte måneder
                                 * > når medlemmet skal vurderes for uføretrygd.
                                 *
                                 * Dagens praksis i Arena er dato-til-dato, men regelspesifiseringen gir ingen spesifikasjon.
                                 */
                        utledVarighetSluttdato(it)
                    },
                ) { varighetsvurdering, vurdering ->
                    when (varighetsvurdering) {
                        Varighetsvurdering.VARIGHET_OK ->
                            if (vurdering.harRettPåAAPMedOvergangUføre()) {
                                Vilkårsvurdering(
                                    utfall = Utfall.OPPFYLT,
                                    begrunnelse = vurdering.begrunnelse,
                                    innvilgelsesårsak = null,
                                    faktagrunnlag = grunnlag,
                                    manuellVurdering = true,
                                )
                            } else {
                                Vilkårsvurdering(
                                    utfall = Utfall.IKKE_OPPFYLT,
                                    begrunnelse = vurdering.begrunnelse,
                                    innvilgelsesårsak = null,
                                    avslagsårsak = Avslagsårsak.IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE,
                                    faktagrunnlag = grunnlag,
                                    manuellVurdering = true,
                                )
                            }

                        Varighetsvurdering.VARIGHET_OVERSKREDET ->
                            Vilkårsvurdering(
                                utfall = Utfall.IKKE_OPPFYLT,
                                begrunnelse = null,
                                innvilgelsesårsak = null,
                                avslagsårsak = Avslagsårsak.VARIGHET_OVERSKREDET_OVERGANG_UFORE,
                                faktagrunnlag = grunnlag,
                                manuellVurdering = false,
                            )
                    }
                }

        vilkår.leggTilVurderinger(vurderinger)
    }

}