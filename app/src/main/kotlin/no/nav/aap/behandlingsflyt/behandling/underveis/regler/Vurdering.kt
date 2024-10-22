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
    internal val soningsVurdering: SoningVurdering? = null,
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

    fun leggTilSoningsVurdering(vurdering: SoningVurdering): Vurdering {
        return copy(soningsVurdering = vurdering)
    }

    fun leggTilAktivtBidragVurdering(vurdering: AktivtBidragVurdering): Vurdering {
        return copy(aktivtBidragVurdering = vurdering)
    }

    fun vurderinger(): Map<Vilkårtype, Utfall> {
        return vurderinger.toMap()
    }

    fun harRett(): Boolean {
        return ingenVilkårErAvslått() && arbeiderMindreEnnGrenseverdi() && harOverholdtMeldeplikten() && sonerIkke()
    }

    private fun sonerIkke(): Boolean {
        if (soningsVurdering == null) {
            return true
        }
        return !soningsVurdering.girOpphør
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
        if (gradering == null) {
            return null
        }
        if (harRett()) {
            if (institusjonVurdering?.skalReduseres == true) {
                return Gradering(
                    totaltAntallTimer = gradering.totaltAntallTimer,
                    andelArbeid = gradering.andelArbeid,
                    gradering = gradering.gradering.minus(
                        Prosent.`50_PROSENT`
                    )
                )
            }
            return gradering
        } else {
            return Gradering(
                totaltAntallTimer = gradering.totaltAntallTimer,
                andelArbeid = gradering.andelArbeid,
                gradering = Prosent.`0_PROSENT`
            )
        }
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
        } else if (!sonerIkke()) {
            return UnderveisÅrsak.SONER_STRAFF
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

    override fun toString(): String {
        return """
            Vurdering(
            harRett=${harRett()},
            meldeplikt=${meldepliktVurdering?.utfall ?: Utfall.IKKE_VURDERT}(${meldepliktVurdering?.årsak ?: "-"}),
            gradering=${gradering?.gradering ?: Prosent(0)},
            bruddAktivitetsplikt=${fraværFastsattAktivitetVurdering}
            institusjonVurdering=${institusjonVurdering}
            grenseverdi=${grenseverdi}
            )""".trimIndent().replace("\n", "")
    }

}
