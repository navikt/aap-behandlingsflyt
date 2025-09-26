package no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre

import no.nav.aap.behandlingsflyt.behandling.vilkår.Varighetsvurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.mapMedDatoTilDatoVarighet
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

class OvergangUføreVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<OvergangUføreFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGUFØREVILKÅRET)
    override fun vurder(grunnlag: OvergangUføreFaktagrunnlag) {
        val vurderinger = grunnlag.vurderinger
            .sortedBy { it.opprettet }
            .map { vurdering ->
                val fom = listOfNotNull(
                    vurdering.virkningsdato,
                    grunnlag.rettighetsperiode.fom,
                ).max()
                Tidslinje(Periode(fom, Tid.MAKS), vurdering)
            }
            .fold(Tidslinje<OvergangUføreVurdering>()) { t1, t2 ->
                t1.kombiner(t2, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }
            .begrensetTil(grunnlag.rettighetsperiode)
            .komprimer()
            .mapMedDatoTilDatoVarighet(
                harBegrensetVarighet = { it.harRettPåAAPMedOvergangUføre() },
                varighet = {
                    /* Fra lovteksten § 11-18:
                     * > Det kan gis arbeidsavklaringspenger i inntil åtte måneder
                     * > når medlemmet skal vurderes for uføretrygd.
                     *
                     * Dagens praksis i Arena er dato-til-dato, men regelspesifiseringen gir ingen spesifikasjon.
                     */
                    it.plusMonths(8).minusDays(1)
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

    enum class UføreSøknadVedtak(val verdi: String) {
        JA_AVSLAG("JA_AVSLAG"), JA_GRADERT("JA_GRADERT"), JA_FULL("JA_FULL"), NEI("NEI")
    }
}
