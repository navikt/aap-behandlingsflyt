package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class Element<T>(val fom: LocalDate?, val tom: LocalDate?, val verdi: T)

data class TidslinjeDto<T>(
    val elementer: List<Element<T>>
) {

    constructor(tidslinje: Tidslinje<T>) : this(
        tidslinje.segmenter()
            .map { segment ->
                Element(
                    fom = segment.periode.fom.takeIf { it > Tid.MIN },
                    tom = segment.periode.tom.takeIf { it < Tid.MAKS },
                    verdi = segment.verdi
                )
            }
    )
}

