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


/**
 * Grupperer opphold i sammenhengende kjeder og beregner tidligste reduksjonsdato per kjede.
 * Sammenhengende opphold (jf. [grupperSammenhengendeOppholdSegmenter]) behandles som ett opphold
 * med kjedens fulle periode (fra første til siste segment) når reduksjonsdatoen beregnes,
 * slik at f.eks. to korte opphold som til sammen varer 3 måneder ikke feilaktig gir umiddelbar
 * reduksjon for et senere opphold innenfor 3-månedersgrensen.
 */
fun beregnTidligsteReduksjonsdatoPerKjede(
    opphold: List<Segment<Institusjon>>,
    harInnvilgetReduksjon: (SammenhengendeOppholdGruppe) -> Boolean = { false },
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
