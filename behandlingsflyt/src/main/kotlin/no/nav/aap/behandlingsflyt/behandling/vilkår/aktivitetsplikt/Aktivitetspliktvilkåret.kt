package no.nav.aap.behandlingsflyt.behandling.vilkår.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Aktivitetsplikt11_7Regel.Companion.tilAktivitetspliktVurderingTidslinje
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall.OPPHØR
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall.STANS
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class AktivitetspliktvilkåretGrunnlag(
    val aktivitetsplikt117grunnlag: Aktivitetsplikt11_7Grunnlag,
    val vurderFra: LocalDate,
) : Faktagrunnlag

class Aktivitetspliktvilkåret(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<AktivitetspliktvilkåretGrunnlag> {
    private val vilkåret = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.AKTIVITETSPLIKT)

    override fun vurder(grunnlag: AktivitetspliktvilkåretGrunnlag) {
        val rettighetsperiode = Periode(grunnlag.vurderFra, Tid.MAKS)

        val utgangspunktOppfylt = tidslinjeOf(
            rettighetsperiode to Vilkårsvurdering(
                utfall = Utfall.OPPFYLT,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
            )
        )

        val saksbehandlersVurderinger =
            grunnlag.aktivitetsplikt117grunnlag.tilAktivitetspliktVurderingTidslinje(rettighetsperiode)
                .map {
                    Vilkårsvurdering(
                        utfall = when {
                            it.vurdering.erOppfylt -> Utfall.OPPFYLT
                            else -> Utfall.IKKE_OPPFYLT
                        },
                        manuellVurdering = true,
                        begrunnelse = it.vurdering.begrunnelse,
                        avslagsårsak = when (it.vurdering.utfall) {
                            STANS -> Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS
                            OPPHØR -> Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR
                            null -> null
                        },
                        faktagrunnlag = grunnlag,
                    )
                }

        vilkåret.leggTilVurderinger(
            utgangspunktOppfylt.mergePrioriterHøyre(saksbehandlersVurderinger)
        )
    }
}