package no.nav.aap.misc.institusjonsopphold

import java.time.LocalDateTime
import no.nav.aap.institusjonsopphold.Soningsvurdering
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid

data class Soningsvurderinger(
    val id: Long? = null,
    val vurderinger: List<Soningsvurdering>,
    val vurdertAv: Bruker,
    val vurdertTidspunkt: LocalDateTime
) {
    fun tilTidslinje(): Tidslinje<Soningsvurdering> =
        vurderinger
            .sortedBy { it.fraDato }
            .somTidslinje { Periode(it.fraDato, Tid.MAKS) }
}