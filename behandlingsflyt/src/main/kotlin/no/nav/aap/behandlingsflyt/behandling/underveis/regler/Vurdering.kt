package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

data class Vurdering(
    internal val fårAapEtter: RettighetsType? = null,
    internal val meldepliktVurdering: MeldepliktVurdering? = null,
    internal val aktivitetspliktVurdering: AktivitetspliktVurdering? = null,
    internal val oppholdskravVurdering: OppholdskravUnderveisVurdering? = null,
    private val gradering: ArbeidsGradering? = null,
    private val samordningProsent: Prosent? = null,
    private val grenseverdi: Prosent? = null,
    internal val institusjonVurdering: InstitusjonVurdering? = null,
    internal val soningsVurdering: SoningVurdering? = null,
    private val meldeperiode: Periode? = null,
    val varighetVurdering: VarighetVurdering? = null,
    private val reduksjonArbeidOverGrenseEnabled: Boolean? = null,
) {
    fun leggTilRettighetstype(rettighetstype: RettighetsType): Vurdering {
        return copy(fårAapEtter = rettighetstype)
    }

    fun leggTilGradering(arbeidsGradering: ArbeidsGradering): Vurdering {
        return copy(gradering = arbeidsGradering)
    }

    fun leggTilMeldepliktVurdering(meldepliktVurdering: MeldepliktVurdering): Vurdering {
        return copy(meldepliktVurdering = meldepliktVurdering)
    }

    fun leggTilGrenseverdi(grenseverdi: Prosent): Vurdering {
        return copy(grenseverdi = grenseverdi)
    }

    fun leggTilInstitusjonVurdering(vurdering: InstitusjonVurdering): Vurdering {
        return copy(institusjonVurdering = vurdering)
    }

    fun leggTilSoningsVurdering(vurdering: SoningVurdering): Vurdering {
        return copy(soningsVurdering = vurdering)
    }

    fun leggTilAktivitetspliktVurdering(vurdering: AktivitetspliktVurdering): Vurdering {
        return copy(aktivitetspliktVurdering = vurdering)
    }

    fun leggTilMeldeperiode(meldeperiode: Periode): Vurdering {
        return copy(meldeperiode = meldeperiode)
    }

    fun leggTilVarighetVurdering(varighetVurdering: VarighetVurdering): Vurdering {
        return copy(varighetVurdering = varighetVurdering)
    }

    fun leggTilOppholdskravVurdering(oppholdskravVurdering: OppholdskravUnderveisVurdering): Vurdering {
        return copy(oppholdskravVurdering = oppholdskravVurdering)
    }

    private fun bryterAktivitetsplikt11_7(): Boolean {
        return stansEtterBruddPåAktivitetsplikt11_7() || opphørEtterBruddPåAktivitetsplikt11_7()
    }

    private fun stansEtterBruddPåAktivitetsplikt11_7(): Boolean {
        return aktivitetspliktVurdering?.vilkårsvurdering == AktivitetspliktVurdering.Vilkårsvurdering.BRUDD_AKTIVITETSPLIKT_11_7_STANS
    }

    private fun opphørEtterBruddPåAktivitetsplikt11_7(): Boolean {
        return aktivitetspliktVurdering?.vilkårsvurdering == AktivitetspliktVurdering.Vilkårsvurdering.BRUDD_AKTIVITETSPLIKT_11_7_OPPHØR
    }

    private fun bryterOppholdskrav(): Boolean {
        return opphørEtterOppholdskrav() || stansEtterOppholdskrav()
    }

    private fun opphørEtterOppholdskrav(): Boolean {
        return oppholdskravVurdering?.vilkårsvurdering == OppholdskravUnderveisVurdering.Vilkårsvurdering.BRUDD_OPPHOLDSKRAV_11_3_OPPHØR
    }

    private fun stansEtterOppholdskrav(): Boolean {
        return oppholdskravVurdering?.vilkårsvurdering == OppholdskravUnderveisVurdering.Vilkårsvurdering.BRUDD_OPPHOLDSKRAV_11_3_STANS
    }

    fun harRett(): Boolean {
        return fårAapEtter != null &&
                (when (reduksjonArbeidOverGrenseEnabled) {
                    null, false -> arbeiderMindreEnnGrenseverdi()
                    true -> true
                }) &&
                harOverholdtMeldeplikten() &&
                sonerIkke() &&
                !bryterAktivitetsplikt11_7() &&
                !bryterOppholdskrav() &&
                varighetsvurderingOppfylt()
    }

    private fun varighetsvurderingOppfylt(): Boolean {
        return varighetVurdering !is Avslag
    }

    private fun sonerIkke(): Boolean {
        if (soningsVurdering == null) {
            return true
        }
        return !soningsVurdering.girOpphør
    }

    private fun harOverholdtMeldeplikten(): Boolean {
        val utfall = meldepliktVurdering?.utfall
        return utfall == null || utfall == OPPFYLT
    }

    private fun arbeiderMindreEnnGrenseverdi(): Boolean {
        return gradering == null || grenseverdi == null || grenseverdi() >= gradering.andelArbeid
    }

    fun rettighetsType(): RettighetsType? {
        return fårAapEtter
    }

    fun grenseverdi(): Prosent {
        return requireNotNull(grenseverdi)
    }

    fun meldeperiode(): Periode {
        return requireNotNull(meldeperiode)
    }

    fun arbeidsgradering(): ArbeidsGradering {
        return when {
            gradering == null -> error("gradering er ikke lagt til enda")
            else -> gradering
        }
    }

    fun utfall(): Utfall {
        return if (harRett()) {
            OPPFYLT
        } else {
            Utfall.IKKE_OPPFYLT
        }
    }

    fun avslagsårsak(): UnderveisÅrsak? {
        if (harRett()) {
            return null
        }

        if (fårAapEtter == null) {
            return UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT
        } else if (stansEtterOppholdskrav()) {
            return UnderveisÅrsak.BRUDD_PÅ_OPPHOLDSKRAV_11_3_STANS
        } else if (opphørEtterOppholdskrav()) {
            return UnderveisÅrsak.BRUDD_PÅ_OPPHOLDSKRAV_11_3_OPPHØR
        } else if (!sonerIkke()) {
            return UnderveisÅrsak.SONER_STRAFF
        } else if (opphørEtterBruddPåAktivitetsplikt11_7()) {
            return UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT_11_7_OPPHØR
        } else if (stansEtterBruddPåAktivitetsplikt11_7()) {
            return UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT_11_7_STANS
        } else if (reduksjonArbeidOverGrenseEnabled != true && !arbeiderMindreEnnGrenseverdi()) {
            return UnderveisÅrsak.ARBEIDER_MER_ENN_GRENSEVERDI
        } else if (!harOverholdtMeldeplikten()) {
            return requireNotNull(meldepliktVurdering?.årsak)
        } else if (!varighetsvurderingOppfylt()) {
            return UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP
        }
        throw IllegalStateException("Ukjent avslagsårsak")
    }

    fun skalReduseresDagsatser(): Boolean {
//        if (!harRett() || fraværFastsattAktivitetVurdering?.utfall == UNNTAK) {
//            return false
//        }
//        return reduksjonAktivitetspliktVurdering?.vilkårsvurdering == VILKÅR_FOR_REDUKSJON_OPPFYLT
        return false
    }

    override fun toString(): String {
        return """
            Vurdering(
            harRett=${harRett()},
            meldeplikt=${meldepliktVurdering},
            gradering=${gradering?.gradering ?: Prosent(0)},
            aktivitetsplikt11_7=${aktivitetspliktVurdering},
            institusjonVurdering=${institusjonVurdering},
            oppholdskravVurdering=${oppholdskravVurdering},
            grenseverdi=${grenseverdi}
            )""".trimIndent().replace("\n", "")
    }

}

fun <T> Tidslinje<Vurdering>.leggTilVurderinger(
    vurderinger: Tidslinje<T>, utvidVurdering: (Vurdering, T) -> Vurdering
): Tidslinje<Vurdering> {
    return vurderinger.kombiner(
        this,
        JoinStyle.OUTER_JOIN
        { periode, nyVurdering, foreløpigVurdering ->
            if (nyVurdering == null) return@OUTER_JOIN foreløpigVurdering
            val vurdering = utvidVurdering(foreløpigVurdering?.verdi ?: Vurdering(), nyVurdering.verdi)
            Segment(periode, vurdering)
        },
    )
}
