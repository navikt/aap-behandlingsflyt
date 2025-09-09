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

    fun leggTilGrenseverdi(grenseverdi: Prosent): Vurdering {
        return copy(grenseverdi = grenseverdi)
    }

    fun leggTilInstitusjonVurdering(vurdering: InstitusjonVurdering): Vurdering {
        return copy(institusjonVurdering = vurdering)
    }

    fun leggTilSoningsVurdering(vurdering: SoningVurdering): Vurdering {
        return copy(soningsVurdering = vurdering)
    }

    fun leggTilMeldeperiode(meldeperiode: Periode): Vurdering {
        return copy(meldeperiode = meldeperiode)
    }

    fun leggTilVarighetVurdering(varighetVurdering: VarighetVurdering): Vurdering {
        return copy(varighetVurdering = varighetVurdering)
    }
    
    fun harRett(): Boolean {
        return fårAapEtter != null &&
                arbeiderMindreEnnGrenseverdi() &&
                harOverholdtMeldeplikten() &&
                sonerIkke() &&
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
        } else if (!sonerIkke()) {
            return UnderveisÅrsak.SONER_STRAFF
        } else if (!arbeiderMindreEnnGrenseverdi()) {
            return UnderveisÅrsak.ARBEIDER_MER_ENN_GRENSEVERDI
        } else if (!harOverholdtMeldeplikten()) {
            return requireNotNull(meldepliktVurdering?.årsak)
        } else if (!varighetsvurderingOppfylt()) {
            return UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP
        }
        throw IllegalStateException("Ukjent avslagsårsak")
    }

    fun skalReduseresDagsatser(): Boolean {
        // TODO: Aktivitetspliktvurdering
        return false
    }

    override fun toString(): String {
        return """
            Vurdering(
            harRett=${harRett()},
            meldeplikt=${meldepliktVurdering},
            gradering=${gradering?.gradering ?: Prosent(0)},
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
