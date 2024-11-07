package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.komponenter.type.Periode

class SykdomsvilkårFraLansering(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<SykdomsFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

    override fun vurder(grunnlag: SykdomsFaktagrunnlag) {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak? = null

        val sykdomsvurdering = grunnlag.sykdomsvurdering
        val studentVurdering = grunnlag.studentvurdering
        val yrkesskadevurdering = grunnlag.yrkesskadevurdering


        if (studentVurdering?.erOppfylt() == true) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.STUDENT
        } else if (harSykdomBlittVurdertTilGodkjent(sykdomsvurdering, yrkesskadevurdering)) {
            utfall = Utfall.OPPFYLT
            if (yrkesskadevurdering != null && yrkesskadevurdering.erÅrsakssammenheng) {
                innvilgelsesårsak = Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG
            }
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = if (sykdomsvurdering?.erSkadeSykdomEllerLyteVesentligdel == false) {
                Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL
            } else if (sykdomsvurdering?.erNedsettelseIArbeidsevneMerEnnHalvparten == false) {
                Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE
            } else {
                Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO noe mer rett
            }
        }

        lagre(
            grunnlag,
            VurderingsResultat(
                utfall = utfall,
                avslagsårsak = avslagsårsak,
                innvilgelsesårsak = innvilgelsesårsak
            )
        )
    }

    private fun harSykdomBlittVurdertTilGodkjent(
        sykdomsvurdering: Sykdomsvurdering?,
        yrkesskadevurdering: Yrkesskadevurdering?
    ): Boolean {
        if (sykdomsvurdering == null) {
            return false
        }
        return sykdomsvurdering.erSkadeSykdomEllerLyteVesentligdel == true &&
                sykdomsvurdering.erArbeidsevnenNedsatt == true &&
                sykdomsvurdering.erNedsettelseIArbeidsevneAvEnVissVarighet == true &&
                (sykdomsvurdering.erNedsettelseIArbeidsevneMerEnnHalvparten == true ||
                        (sykdomsvurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true && yrkesskadevurdering?.erÅrsakssammenheng == true))
    }

    private fun lagre(grunnlag: SykdomsFaktagrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
                vurderingsResultat.utfall,
                false,
                null,
                vurderingsResultat.innvilgelsesårsak,
                vurderingsResultat.avslagsårsak,
                grunnlag,
                vurderingsResultat.versjon()
            )
        )

        return vurderingsResultat
    }

}
