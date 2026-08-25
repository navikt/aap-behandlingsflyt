package no.nav.aap.behandlingsflyt.behandling.vilkår.aktivitetsplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall.OPPHØR
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall.STANS
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class AktivitetspliktvilkåretGrunnlag(
    val aktivitetsplikt117grunnlag: Aktivitetsplikt11_7Grunnlag,
    val vurderFra: LocalDate,
) : Faktagrunnlag

object Aktivitetspliktvilkåret : Vilkårsvurderer<AktivitetspliktvilkåretGrunnlag> {
    override val vilkårtype = Vilkårtype.AKTIVITETSPLIKT

    override fun vurder(faktagrunnlag: AktivitetspliktvilkåretGrunnlag): Tidslinje<Vilkårsvurdering> {
        val rettighetsperiode = Periode(faktagrunnlag.vurderFra, Tid.MAKS)

        val utgangspunktOppfylt = tidslinjeOf(
            rettighetsperiode to Vilkårsvurdering(
                utfall = Utfall.OPPFYLT,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = faktagrunnlag,
            )
        )

        val saksbehandlersVurderinger =
            faktagrunnlag.aktivitetsplikt117grunnlag
                .tidslinje()
                .map {
                    Vilkårsvurdering(
                        utfall = when {
                            it.erOppfylt -> Utfall.OPPFYLT
                            else -> Utfall.IKKE_OPPFYLT
                        },
                        manuellVurdering = true,
                        begrunnelse = it.begrunnelse,
                        avslagsårsak = when (it.utfall) {
                            STANS -> Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS
                            OPPHØR -> Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR
                            null -> null
                        },
                        faktagrunnlag = faktagrunnlag,
                    )
                }

        return utgangspunktOppfylt.mergePrioriterHøyre(saksbehandlersVurderinger)
    }
}