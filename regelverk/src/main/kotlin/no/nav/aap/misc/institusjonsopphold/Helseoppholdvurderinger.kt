package no.nav.aap.misc.institusjonsopphold

import java.time.LocalDateTime
import no.nav.aap.institusjonsopphold.HelseinstitusjonVurdering
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje

data class Helseoppholdvurderinger(
    val id: Long?,
    val vurderinger: List<HelseinstitusjonVurdering>,
    val vurdertTidspunkt: LocalDateTime
) {
    fun tilTidslinje(): Tidslinje<HelseinstitusjonVurdering> = vurderinger.somTidslinje { it.periode }
}