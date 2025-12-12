package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent

data class UnderveisperiodeId(val asLong: Long)

/**
 * @param institusjonsoppholdReduksjon Hvor mange prosent institusjonsopphold skal redusere. Merk: ikke prosentpoeng.
 */
data class Underveisperiode(
    val periode: Periode,
    val meldePeriode: Periode,
    val utfall: Utfall,
    val rettighetsType: RettighetsType?,
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

    override fun compareTo(other: Underveisperiode): Int {
        return periode.compareTo(other.periode)
    }

    companion object
}