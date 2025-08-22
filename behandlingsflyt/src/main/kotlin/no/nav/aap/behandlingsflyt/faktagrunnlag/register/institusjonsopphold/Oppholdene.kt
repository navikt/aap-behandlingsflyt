package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.komponenter.tidslinje.Segment
import java.util.Objects

class Oppholdene(
    val id: Long?,
    opphold: List<Segment<Institusjon>>
) {
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