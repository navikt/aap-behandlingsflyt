package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingDto
import no.nav.aap.komponenter.tidslinje.Segment
import java.time.LocalDate

fun lagOppholdId(institusjonNavn: String, fom: LocalDate): String =
    "${institusjonNavn}::${fom}"

fun HelseinstitusjonVurdering.giReduksjon(): Boolean =
    !erHistoriskUtenReduksjonsberegning &&
            faarFriKostOgLosji &&
            forsoergerEktefelle == false &&
            harFasteUtgifter == false

fun HelseinstitusjonVurderingDto.giReduksjon(): Boolean =
    !erHistoriskUtenReduksjonsberegning &&
            faarFriKostOgLosji &&
            forsoergerEktefelle == false &&
            harFasteUtgifter == false


fun beregnTidligsteReduksjonsdatoPerKjede(
    opphold: List<Segment<Institusjon>>,
    harInnvilgetReduksjon: (SammenhengendeOppholdGruppe) -> Boolean = { false }
): Map<SammenhengendeOppholdGruppe, LocalDate> {
    if (opphold.isEmpty()) return emptyMap()

    val kjeder = grupperSammenhengendeOppholdSegmenter(opphold).sortedBy { it.periode.fom }
    val result = mutableMapOf<SammenhengendeOppholdGruppe, LocalDate>()

    kjeder.forEachIndexed { index, kjede ->
        result[kjede] = if (index == 0) {
            kjede.periode.fom.withDayOfMonth(1).plusMonths(4)
        } else {
            val forrige = kjeder[index - 1]
            val erInnenTreMåneder = !kjede.periode.fom.isAfter(forrige.periode.tom.plusMonths(3))

            if (erInnenTreMåneder && harInnvilgetReduksjon(forrige)) {
                kjede.periode.fom.withDayOfMonth(1).plusMonths(1) // 1-månedsregelen
            } else {
                kjede.periode.fom.withDayOfMonth(1).plusMonths(4) // 3-månedersregelen
            }
        }
    }
    return result
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