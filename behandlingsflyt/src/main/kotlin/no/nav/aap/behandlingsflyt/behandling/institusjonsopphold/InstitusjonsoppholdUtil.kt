package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.komponenter.tidslinje.Segment
import java.time.LocalDate

fun lagOppholdId(institusjonNavn: String, fom: LocalDate): String =
    "${institusjonNavn}::${fom}"

/**
 * Grupperer opphold i sammenhengende kjeder og beregner tidligste reduksjonsdato per kjede.
 * Sammenhengende opphold (jf. [grupperSammenhengendeOppholdSegmenter]) behandles som ett opphold
 * med kjedens fulle periode (fra første til siste segment) når reduksjonsdatoen beregnes,
 * slik at f.eks. to korte opphold som til sammen varer 3 måneder ikke feilaktig gir umiddelbar
 * reduksjon for et senere opphold innenfor 3-månedersgrensen.
 */
fun beregnTidligsteReduksjonsdatoPerKjede(
    opphold: List<Segment<Institusjon>>
): Map<SammenhengendeOppholdGruppe, LocalDate> {
    if (opphold.isEmpty()) return emptyMap()

    val kjeder = grupperSammenhengendeOppholdSegmenter(opphold)

    // Ett representant-segment per kjede: kjedens fulle periode (første fom - siste tom)
    val kjedeTilRepresentant = kjeder.associateWith { kjede ->
        Segment(kjede.periode, kjede.elementer.first().verdi)
    }

    val tidligsteReduksjonsdatoPerRepresentant =
        beregnTidligsteReduksjonsdatoPerOpphold(kjedeTilRepresentant.values.toList())

    return kjedeTilRepresentant.mapNotNull { (kjede, representant) ->
        tidligsteReduksjonsdatoPerRepresentant[representant]?.let { kjede to it }
    }.toMap()
}

fun beregnTidligsteReduksjonsdatoPerOpphold(
    opphold: List<Segment<Institusjon>>
): Map<Segment<Institusjon>, LocalDate> {
    if (opphold.isEmpty()) return emptyMap()

    val sortert = opphold.sortedBy { it.periode.fom }
    val result = mutableMapOf<Segment<Institusjon>, LocalDate>()

    sortert.forEachIndexed { index, nåværende ->
        val tidligsteReduksjonsdato = if (index == 0) {
            // Første opphold: innleggelsesmåned + 3 måneder (dvs. 1. dag i måned 4 etter innleggelse)
            nåværende.periode.fom.withDayOfMonth(1).plusMonths(4)
        } else {
            val forrige = sortert[index - 1]
            val treMånederEtterForrigeUtskrivelse = forrige.periode.tom.plusMonths(3)
            val erInnenTreMåneder = !nåværende.periode.fom.isAfter(treMånederEtterForrigeUtskrivelse)

            if (erInnenTreMåneder) {
                // Nytt opphold innen tre måneder: reduksjon kan starte fra innleggelsesdato
                nåværende.periode.fom
            } else {
                // Mer enn tre måneder siden forrige: innleggelsesmåned + 3 måneder
                nåværende.periode.fom.withDayOfMonth(1).plusMonths(4)
            }
        }
        result[nåværende] = tidligsteReduksjonsdato
    }

    return result
}