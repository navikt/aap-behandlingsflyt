package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class Helseoppholdvurderinger(val id: Long?, val vurderinger: List<HelseinstitusjonVurdering>) {
    fun tilTidslinje(): Tidslinje<HelseinstitusjonVurdering> {
        return Tidslinje(vurderinger.map { Segment(it.periode, it) })
    }
}
