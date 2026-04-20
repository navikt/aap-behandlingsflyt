package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import java.util.*

data class UnderveisperiodeId(val asLong: Long)

/**
 * @param institusjonsoppholdReduksjon Hvor mange prosent institusjonsopphold skal redusere. Merk: ikke prosentpoeng.
 */
class Underveisperiode(
    val periode: Periode,
    val meldePeriode: Periode,
    val utfall: Utfall,
    rettighetsType: RettighetsType?,
    val avslagsårsak: UnderveisÅrsak?,
    val grenseverdi: Prosent,
    val institusjonsoppholdReduksjon: Prosent,
    val arbeidsgradering: ArbeidsGradering,
    val trekk: Dagsatser,
    val brukerAvKvoter: Set<Kvote>,
    val meldepliktStatus: MeldepliktStatus?,
    val meldepliktGradering: Prosent?,
    val id: UnderveisperiodeId? = null,
) : Comparable<Underveisperiode> {
    init {
        if (utfall == Utfall.IKKE_OPPFYLT) requireNotNull(avslagsårsak) { "Må ha avslagsårsak om utfall ikke oppfylt." }
        if (utfall == Utfall.OPPFYLT) requireNotNull(rettighetsType) { "Må ha rettighetsType om utfall oppfylt." }
    }

    /** Tidligere lagret [UnderveisService][no.nav.aap.behandlingsflyt.behandling.underveis.UnderveisService] ned [rettighetsType] også for perioder uten rett til AAP.
     * Nå lagrer den kun ned hvis medlemmet faktisk har rett på AAP. Denne getteren gjør at vi ikke trenger å ta hensyn til den forskjellen. */
    val rettighetsType: RettighetsType? = if (utfall == Utfall.OPPFYLT) rettighetsType else null

    override fun compareTo(other: Underveisperiode): Int {
        return periode.compareTo(other.periode)
    }

    fun copy(
        periode: Periode,
        id: UnderveisperiodeId?,
        meldepliktStatus: MeldepliktStatus?,
    ) = Underveisperiode(
        periode = periode,
        meldePeriode = meldePeriode,
        utfall = utfall,
        rettighetsType = rettighetsType,
        avslagsårsak = avslagsårsak,
        grenseverdi = grenseverdi,
        institusjonsoppholdReduksjon = institusjonsoppholdReduksjon,
        arbeidsgradering = arbeidsgradering,
        trekk = trekk,
        brukerAvKvoter = brukerAvKvoter,
        meldepliktStatus = meldepliktStatus,
        meldepliktGradering = meldepliktGradering,
        id = id,
    )

    override fun equals(other: Any?) =
        other is Underveisperiode &&
                this.periode == other.periode &&
                this.meldePeriode == other.meldePeriode &&
                this.utfall == other.utfall &&
                this.rettighetsType == other.rettighetsType &&
                this.avslagsårsak == other.avslagsårsak &&
                this.grenseverdi == other.grenseverdi &&
                this.institusjonsoppholdReduksjon == other.institusjonsoppholdReduksjon &&
                this.arbeidsgradering == other.arbeidsgradering &&
                this.trekk == other.trekk &&
                this.brukerAvKvoter == other.brukerAvKvoter &&
                this.meldepliktStatus == other.meldepliktStatus &&
                this.meldepliktGradering == other.meldepliktGradering

    override fun hashCode() = Objects.hash(
        periode, meldePeriode, utfall, rettighetsType, avslagsårsak, grenseverdi,
        institusjonsoppholdReduksjon, arbeidsgradering, trekk, brukerAvKvoter,
        meldepliktStatus, meldepliktGradering,
    )

    override fun toString() = """
        Underveisperiode(
            periode = $periode,
            meldePeriode = $meldePeriode,
            utfall = $utfall,
            rettighetsType = $rettighetsType,
            avslagsårsak = $avslagsårsak,
            grenseverdi = $grenseverdi,
            institusjonsoppholdReduksjon = $institusjonsoppholdReduksjon,
            arbeidsgradering = $arbeidsgradering,
            trekk = $trekk,
            brukerAvKvoter = $brukerAvKvoter,
            meldepliktStatus = $meldepliktStatus,
            meldepliktGradering = $meldepliktGradering,
            id = $id,
        )
        """.trimIndent()
}