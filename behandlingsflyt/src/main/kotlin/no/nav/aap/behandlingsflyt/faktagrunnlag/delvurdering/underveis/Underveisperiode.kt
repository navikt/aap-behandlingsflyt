package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent

data class UnderveisperiodeId(val asLong: Long)

/**
 * @param samordningGradering
 */
data class Underveisperiode(
    val periode: Periode,
    val meldePeriode: Periode,
    val utfall: Utfall,
    val rettighetsType: RettighetsType?,
    val avslagsårsak: UnderveisÅrsak?,
    val grenseverdi: Prosent,
    // fjerne denne, så kan beregntilkjentytelse hente den inn selv
    val samordningGradering: Prosent,
    val institusjonsoppholdReduksjon: Prosent,
    val arbeidsgradering: ArbeidsGradering,
    val trekk: Dagsatser,
    val brukerAvKvoter: Set<Kvote>,
    val bruddAktivitetspliktId: BruddAktivitetspliktId?,
    val id: UnderveisperiodeId? = null,
) : Comparable<Underveisperiode> {

    @Deprecated("dddd")
    fun utbetalingsgrad(): Prosent {
        // Ta hensyn til inst-opphold, arbeid, og samordning
        if (utfall == Utfall.IKKE_OPPFYLT) {
            return Prosent.`0_PROSENT`
        }
        val minusInstitusjonsoppholdreduksjon = arbeidsgradering.gradering.minus(institusjonsoppholdReduksjon)
        val res = minusInstitusjonsoppholdreduksjon.minus(samordningGradering)
        return res
    }

    override fun compareTo(other: Underveisperiode): Int {
        return periode.compareTo(other.periode)
    }
}