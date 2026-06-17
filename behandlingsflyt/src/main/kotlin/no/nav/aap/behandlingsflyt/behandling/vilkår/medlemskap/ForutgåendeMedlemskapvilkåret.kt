package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.forutgåendeMedlemskapNorskOgAvslag
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.type.Periode

class ForutgåendeMedlemskapvilkåret(
    vilkårsresultat: Vilkårsresultat,
    private val rettighetsPeriode: Periode
) : Vilkårsvurderer<ForutgåendeMedlemskapGrunnlag> {
    private val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.MEDLEMSKAP)

    override fun vurder(grunnlag: ForutgåendeMedlemskapGrunnlag) {
        val brukManuellVurderingForForutgåendeMedlemskap =
            grunnlag.medlemskapArbeidInntektGrunnlag?.vurderinger?.isNotEmpty() ?: false

        if (brukManuellVurderingForForutgåendeMedlemskap) {
            val gjeldendeVurderinger = grunnlag.medlemskapArbeidInntektGrunnlag.gjeldendeVurderinger()

            val vilkårsvurderinger = gjeldendeVurderinger
                .map { vurdering ->
                    if (vurdering.oppfyllerForutgåendeMedlemskap()) {
                        Vilkårsvurdering(
                            utfall = Utfall.OPPFYLT,
                            begrunnelse = null,
                            faktagrunnlag = grunnlag,
                            manuellVurdering = true
                        )
                    } else {
                        Vilkårsvurdering(
                            utfall = Utfall.IKKE_OPPFYLT,
                            avslagsårsak = Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE,
                            begrunnelse = null,
                            faktagrunnlag = grunnlag,
                            manuellVurdering = true
                        )
                    }
                }
                .komprimer()
                .begrensetTil(rettighetsPeriode)

            // No-op: av kun norske, hvor mange får nei på manuell vurdering av forutgående medlemskap (11-2)?
            val kunNorskStatsborgerskap =
                grunnlag.personopplysningGrunnlag?.brukerPersonopplysning?.statsborgerskap?.singleOrNull()?.land == "NOR"
            val ikkeoppfyltePerioder = vilkårsvurderinger.filter { it.verdi.utfall == Utfall.IKKE_OPPFYLT }.isNotEmpty()
            prometheus.forutgåendeMedlemskapNorskOgAvslag(kunNorskStatsborgerskap && ikkeoppfyltePerioder).increment()

            vilkår.leggTilVurderinger(vilkårsvurderinger)
        } else if (grunnlag.nyeSoknadGrunnlag == null) {
            val vurderingsResultat = VurderingsResultat(Utfall.IKKE_RELEVANT, null, null)
            leggTilVurdering(grunnlag, vurderingsResultat)
        } else {
            val kanBehandlesAutomatisk = ForutgåendeMedlemskapVurderingService().vurderTilhørighet(
                grunnlag,
                rettighetsPeriode
            ).kanBehandlesAutomatisk
            val utfall = if (kanBehandlesAutomatisk) Utfall.OPPFYLT else Utfall.IKKE_VURDERT
            val vurderingsResultat = VurderingsResultat(utfall, null, null)
            leggTilVurdering(grunnlag, vurderingsResultat)
        }
    }

    fun leggTilYrkesskadeVurdering() {
        val vurderingsResultat = VurderingsResultat(Utfall.OPPFYLT, null, Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG)
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = vurderingsResultat.utfall,
                avslagsårsak = null,
                begrunnelse = vurderingsResultat.innvilgelsesårsak.toString(),
                faktagrunnlag = null,
                versjon = vurderingsResultat.versjon(),
                manuellVurdering = false
            )
        )
    }

    private fun leggTilVurdering(
        grunnlag: ForutgåendeMedlemskapGrunnlag,
        vurderingsResultat: VurderingsResultat,
    ) {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = vurderingsResultat.utfall,
                avslagsårsak = vurderingsResultat.avslagsårsak,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
                versjon = vurderingsResultat.versjon(),
                manuellVurdering = false
            )
        )
    }
}