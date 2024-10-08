package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.Prosent
import java.util.*

data class Vurdering(
    private val vurderinger: EnumMap<Vilkårtype, Utfall> = EnumMap(Vilkårtype::class.java),
    private val meldepliktVurdering: MeldepliktVurdering? = null,
    internal val fraværFastsattAktivitetVurdering: FraværFastsattAktivitetVurdering? = null,
    internal val reduksjonAktivitetspliktVurdering: ReduksjonAktivitetspliktVurdering? = null,
    internal val aktivtBidragVurdering: AktivtBidragVurdering? = null,
    private val gradering: Gradering? = null,
    private val grenseverdi: Prosent? = null,
    internal val institusjonVurdering: InstitusjonVurdering? = null,
) {

    fun leggTilVurdering(vilkårtype: Vilkårtype, utfall: Utfall): Vurdering {
        val kopi = EnumMap(vurderinger)
        kopi[vilkårtype] = utfall
        return copy(vurderinger = kopi)
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

    fun leggTilAktivtBidragVurdering(vurdering: AktivtBidragVurdering): Vurdering {
        return copy(aktivtBidragVurdering = vurdering)
    }

    fun vurderinger(): Map<Vilkårtype, Utfall> {
        return vurderinger.toMap()
    }

    fun harRett(): Boolean {
        return ingenVilkårErAvslått() && arbeiderMindreEnnGrenseverdi() && harOverholdtMeldeplikten()
    }

    private fun harOverholdtMeldeplikten(): Boolean {
        return meldepliktVurdering?.utfall == Utfall.OPPFYLT
    }

    internal fun ingenVilkårErAvslått(): Boolean {
        return vurderinger.isNotEmpty() && vurderinger.none { it.value == Utfall.IKKE_OPPFYLT }
    }

    private fun arbeiderMindreEnnGrenseverdi(): Boolean {
        return gradering == null || grenseverdi() >= gradering.andelArbeid
    }

    fun grenseverdi(): Prosent {
        return requireNotNull(grenseverdi)
    }

    fun gradering(): Gradering? {
        return gradering
    }

    fun utfall(): Utfall {
        return if (harRett()) {
            Utfall.OPPFYLT
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
        } else if (!arbeiderMindreEnnGrenseverdi()) {
            return UnderveisÅrsak.ARBEIDER_MER_ENN_GRENSEVERDI
        } else if (!harOverholdtMeldeplikten()) {
            return requireNotNull(meldepliktVurdering?.årsak)
        }
        throw IllegalStateException("Ukjent avslagsårsak")
    }

    internal fun meldeplikUtfall(): Utfall {
        return meldepliktVurdering?.utfall ?: Utfall.IKKE_VURDERT
    }

    internal fun meldeplikAvslagsårsak(): UnderveisÅrsak? {
        return meldepliktVurdering?.årsak
    }

    fun meldeperiode(): Periode? {
        return meldepliktVurdering?.meldeperiode
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vurdering

        if (vurderinger != other.vurderinger) return false
        if (gradering != other.gradering) return false
        if (meldepliktVurdering != other.meldepliktVurdering) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vurderinger.hashCode()
        result = 31 * result + (gradering?.hashCode() ?: 0)
        result = 31 * result + (meldepliktVurdering?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return """
            Vurdering(
            harRett=${harRett()},
            meldeplikt=${meldepliktVurdering?.utfall ?: Utfall.IKKE_VURDERT}(${meldepliktVurdering?.årsak ?: "-"}),
            gradering=${ gradering?.gradering ?: Prosent( 0) },
            bruddAktivitetsplikt=${fraværFastsattAktivitetVurdering}
            )""".trimIndent().replace("\n", "")
    }

}
