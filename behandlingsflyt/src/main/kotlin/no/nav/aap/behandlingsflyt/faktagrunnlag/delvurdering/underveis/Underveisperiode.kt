package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
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
    val bruddAktivitetspliktId: BruddAktivitetspliktId?,
    val meldepliktStatus: MeldepliktStatus?,
    val id: UnderveisperiodeId? = null,
) : Comparable<Underveisperiode> {
    init {
        if (utfall == Utfall.IKKE_OPPFYLT) requireNotNull(avslagsårsak)
        if (utfall == Utfall.OPPFYLT) requireNotNull(rettighetsType)
    }

    override fun compareTo(other: Underveisperiode): Int {
        return periode.compareTo(other.periode)
    }

    companion object
}