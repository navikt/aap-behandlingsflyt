package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivitetspliktVurdering.Vilkårsvurdering.AKTIVT_BIDRAG_IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Utfall.STANS
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Utfall.UNNTAK
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.VILKÅR_FOR_REDUKSJON_OPPFYLT
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
    internal val fraværFastsattAktivitetVurdering: FraværFastsattAktivitetVurdering? = null,
    internal val reduksjonAktivitetspliktVurdering: ReduksjonAktivitetspliktVurdering? = null,
    internal val aktivitetspliktVurdering: AktivitetspliktVurdering? = null,
    private val gradering: ArbeidsGradering? = null,
    private val samordningProsent: Prosent? = null,
    private val grenseverdi: Prosent? = null,
    internal val institusjonVurdering: InstitusjonVurdering? = null,
    internal val soningsVurdering: SoningVurdering? = null,
    private val meldeperiode: Periode? = null,
    val varighetVurdering: VarighetVurdering? = null,
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
        return fårAapEtter != null &&
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

    fun arbeidsGradering(): ArbeidsGradering {
        return when {
            gradering == null -> error("gradering er ikke lagt til enda")
            harRett() -> gradering
            else -> gradering.copy(gradering = Prosent.`0_PROSENT`)
        }
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
