package no.nav.aap.misc.institusjonsopphold

import java.util.Objects
import no.nav.aap.komponenter.tidslinje.Segment

class Oppholdene(
    val id: Long? = null,
    opphold: List<Segment<Institusjon>>
) {
    constructor(opphold: List<Segment<Institusjon>>) : this(null, opphold)

    val opphold = opphold.sortedWith(
        compareBy<Segment<Institusjon>> { it.periode }
            .thenBy { it.verdi.orgnr }
            .thenBy { it.verdi.kategori }
            .thenBy { it.verdi.type }
            .thenBy { it.verdi.navn }
    )

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Oppholdene) return false

        if (id != null && other.id != null) return id == other.id
        return opphold == other.opphold
    }

    override fun hashCode(): Int {
        return Objects.hash(id, opphold)
    }
}