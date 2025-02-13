package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivitetspliktVurdering.Vilkårsvurdering.AKTIVT_BIDRAG_IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Utfall.STANS
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Utfall.UNNTAK
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.VILKÅR_FOR_REDUKSJON_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

data class Vurdering(
    private val vurderinger: List<EnkelVurdering> = emptyList(),
    internal val meldepliktVurdering: MeldepliktVurdering? = null,
    internal val fraværFastsattAktivitetVurdering: FraværFastsattAktivitetVurdering? = null,
    internal val reduksjonAktivitetspliktVurdering: ReduksjonAktivitetspliktVurdering? = null,
    internal val aktivitetspliktVurdering: AktivitetspliktVurdering? = null,
    private val gradering: Gradering? = null,
    private val grenseverdi: Prosent? = null,
    internal val institusjonVurdering: InstitusjonVurdering? = null,
    internal val soningsVurdering: SoningVurdering? = null,
    private val meldeperiode: Periode? = null,

    val varighetVurdering: VarighetVurdering? = null,
) {

    fun leggTilVurdering(enkelVurdering: EnkelVurdering): Vurdering {
        return copy(vurderinger = vurderinger + enkelVurdering)
    }

    fun fårAapEtter(vilkårtype: Vilkårtype, innvilgelsesårsak: Innvilgelsesårsak?): Boolean {
        // TODO: finn ut om et vilkår kan være oppfylt uten at det er det vilkåret som medlemmet
        // får AAP etter i betydningen fra § 11-12 fjerde ledd. Trenger i så fall prioritering, kanskje?
        return vurderinger.any { it.vilkår == vilkårtype && it.innvilgelsesårsak == innvilgelsesårsak && it.utfall == OPPFYLT }
    }

    fun leggTilGradering(gradering: Gradering): Vurdering {
        return copy(gradering = gradering)
    }

    fun leggTilMeldepliktVurdering(meldepliktVurdering: MeldepliktVurdering): Vurdering {
        return copy(meldepliktVurdering = meldepliktVurdering)
    }

    fun leggTilBruddPåNærmereBestemteAktivitetsplikter(vurdering: ReduksjonAktivitetspliktVurdering): Vurdering {
        return copy(reduksjonAktivitetspliktVurdering = vurdering)
    }

    fun leggTilAktivitetspliktVurdering(fraværFastsattAktivitetVurdering: FraværFastsattAktivitetVurdering): Vurdering {
        return copy(fraværFastsattAktivitetVurdering = fraværFastsattAktivitetVurdering)
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

    fun leggTilAktivtBidragVurdering(vurdering: AktivitetspliktVurdering): Vurdering {
        return copy(aktivitetspliktVurdering = vurdering)
    }

    fun leggTilMeldeperiode(meldeperiode: Periode): Vurdering {
        return copy(meldeperiode = meldeperiode)
    }

    fun leggTilVarighetVurdering(varighetVurdering: VarighetVurdering): Vurdering {
        return copy(varighetVurdering = varighetVurdering)
    }

    private fun bryterAktivitetsplikt(): Boolean {
        return aktivitetspliktVurdering?.vilkårsvurdering == AKTIVT_BIDRAG_IKKE_OPPFYLT
    }

    private fun fraværFastsattAktivitet(): Boolean {
        return fraværFastsattAktivitetVurdering?.utfall == STANS
    }

    fun harRett(): Boolean {
        return ingenVilkårErAvslått() &&
                arbeiderMindreEnnGrenseverdi() &&
                harOverholdtMeldeplikten() &&
                sonerIkke() &&
                !bryterAktivitetsplikt() &&
                !fraværFastsattAktivitet() &&
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

    internal fun ingenVilkårErAvslått(): Boolean {
        return vurderinger.isNotEmpty() && vurderinger.none { it.utfall == Utfall.IKKE_OPPFYLT }
    }

    private fun arbeiderMindreEnnGrenseverdi(): Boolean {
        return gradering == null || grenseverdi() >= gradering.andelArbeid
    }

    fun grenseverdi(): Prosent {
        return requireNotNull(grenseverdi)
    }

    fun meldeperiode(): Periode {
        return requireNotNull(meldeperiode)
    }

    fun gradering(): Gradering {
        return when {
            gradering == null -> error("gradering er ikke lagt til enda")
            harRett() && institusjonVurdering?.skalReduseres == true -> gradering.copy(
                gradering = gradering.gradering.minus(
                    Prosent.`50_PROSENT`,
                )
            )
            harRett() -> gradering
            else -> gradering.copy(gradering = Prosent.`0_PROSENT`)
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

        if (!ingenVilkårErAvslått()) {
            return UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT
        } else if (!sonerIkke()) {
            return UnderveisÅrsak.SONER_STRAFF
        } else if (bryterAktivitetsplikt()) {
            return UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT
        } else if (fraværFastsattAktivitet()) {
            return UnderveisÅrsak.FRAVÆR_FASTSATT_AKTIVITET
        } else if (!arbeiderMindreEnnGrenseverdi()) {
            return UnderveisÅrsak.ARBEIDER_MER_ENN_GRENSEVERDI
        } else if (!harOverholdtMeldeplikten()) {
            return requireNotNull(meldepliktVurdering?.årsak)
        } else if (!varighetsvurderingOppfylt()) {
            return UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP
        }
        throw IllegalStateException("Ukjent avslagsårsak")
    }

    internal fun meldepliktUtfall(): Utfall {
        return meldepliktVurdering?.utfall ?: Utfall.IKKE_VURDERT
    }

    internal fun meldepliktAvslagsårsak(): UnderveisÅrsak? {
        return meldepliktVurdering?.årsak
    }

    fun skalReduseresDagsatser(): Boolean {
        if (!harRett() || fraværFastsattAktivitetVurdering?.utfall == UNNTAK) {
            return false
        }
        return reduksjonAktivitetspliktVurdering?.vilkårsvurdering == VILKÅR_FOR_REDUKSJON_OPPFYLT
    }

    override fun toString(): String {
        return """
            Vurdering(
            harRett=${harRett()},
            meldeplikt=${meldepliktVurdering},
            gradering=${gradering?.gradering ?: Prosent(0)},
            bruddAktivitetsplikt=${fraværFastsattAktivitetVurdering}
            institusjonVurdering=${institusjonVurdering}
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
