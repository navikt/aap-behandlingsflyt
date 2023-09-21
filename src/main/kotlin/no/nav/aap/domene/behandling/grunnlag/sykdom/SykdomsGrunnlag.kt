package no.nav.aap.domene.behandling.grunnlag.sykdom

import no.nav.aap.avklaringsbehov.sykdom.NedreGrense
import no.nav.aap.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.avklaringsbehov.sykdom.Yrkesskadevurdering

class SykdomsGrunnlag(
    val id: Long,
    val behandlingId: Long,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering
) {
    fun erKonsistent(): Boolean {
        //TODO: Clever or not??
        val clever = true
        if (clever) {
            return (yrkesskadevurdering?.erÅrsakssammenheng == true) == (sykdomsvurdering.nedreGrense == NedreGrense.TRETTI)
        }

        if (yrkesskadevurdering?.erÅrsakssammenheng == true) return sykdomsvurdering.nedreGrense == NedreGrense.TRETTI
        return sykdomsvurdering.nedreGrense == NedreGrense.FEMTI
    }
}
