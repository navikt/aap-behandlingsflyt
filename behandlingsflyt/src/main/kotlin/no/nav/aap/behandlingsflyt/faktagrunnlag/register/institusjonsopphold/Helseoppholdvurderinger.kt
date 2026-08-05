package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import java.time.LocalDateTime

data class Helseoppholdvurderinger(
    val id: Long?,
    val vurderinger: List<HelseinstitusjonVurdering>,
    val vurdertTidspunkt: LocalDateTime
) {
    fun tilTidslinje(): Tidslinje<HelseinstitusjonVurdering> = vurderinger.somTidslinje { it.periode }
}