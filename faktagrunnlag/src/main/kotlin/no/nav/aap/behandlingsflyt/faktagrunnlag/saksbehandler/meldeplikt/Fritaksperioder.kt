package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Tid

class Fritaksperioder private constructor(
    vararg fritaksvurderinger: List<Fritaksvurdering>
) {
    constructor(vurderinger: List<Fritaksvurdering>) : this(*arrayOf(vurderinger))

    private val sorterteFritaksvurderinger = fritaksvurderinger.flatMap {
        it.sortedBy { fritaksvurdering -> fritaksvurdering.fraDato }
    }

    infix fun leggTil(nyeFritaksvurderinger: List<Fritaksvurdering>): Fritaksperioder {
        return Fritaksperioder(sorterteFritaksvurderinger, nyeFritaksvurderinger)
    }

    //trenger kanskje ikke å mappe fraDato siden den komprimerer fra l->r uansett + fradato skal ikke kunne flyttes fremover, men tydeligere :shrug:
    fun gjeldendeFritaksvurderinger(): List<Fritaksvurdering> {
        return tidslinje().komprimer().map { it.verdi.copy(fraDato = it.periode.fom) }.trimHead()
    }

    private fun tidslinje(): Tidslinje<Fritaksvurdering> {
        return sorterteFritaksvurderinger.drop(1).fold(sorterteFritaksvurderinger.first().tidslinje()) { acc, fritaksvurdering ->
            acc.kombiner(fritaksvurdering.tidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }
    }

    private fun List<Fritaksvurdering>.trimHead(): List<Fritaksvurdering> {
        if (isEmpty()) return emptyList()
        if (first().harFritak) return this

        return drop(1).trimHead()
    }

    private fun Fritaksvurdering.tidslinje(): Tidslinje<Fritaksvurdering> {
        return Tidslinje(
            listOf(Segment(Periode(fraDato, Tid.MAKS), this))
        )
    }
}