package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import java.time.LocalDateTime

data class Helseoppholdvurderinger(
    val id: Long?,
    val vurderinger: List<HelseinstitusjonVurdering>,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime
) {
    fun tilTidslinje(): Tidslinje<HelseinstitusjonVurdering> = Tidslinje(vurderinger.map { Segment(it.periode, it) })
}