package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class Unntak(
    val status: String,
    val statusaarsak: String?,
    val medlem: Boolean,
    val grunnlag: String,
    val lovvalg: String,
    val helsedel: Boolean,
    val lovvalgsland: String?,
    val kilde: KildesystemMedl?
)

data class MedlemskapDataIntern(
    val unntakId: Number,
    val ident: String,
    // TODO : Fjern string-typing, bruk LocalDate i stedet
    val fraOgMed: String,
    val tilOgMed: String,
    val status: String,
    val statusaarsak: String?,
    val medlem: Boolean,
    val grunnlag: String,
    val lovvalg: String,
    val helsedel: Boolean,
    val lovvalgsland: String?,
    val kilde: KildesystemMedl?
) {
    fun tilDomene(): Segment<Unntak> {
        return Segment(
            Periode(LocalDate.parse(fraOgMed), LocalDate.parse(tilOgMed)),
            Unntak(
                status = status,
                statusaarsak = statusaarsak,
                medlem = medlem,
                grunnlag = grunnlag,
                lovvalg = lovvalg,
                helsedel = helsedel,
                lovvalgsland = lovvalgsland,
                kilde = kilde
            )
        )
    }
}

fun List<MedlemskapDataIntern>.tilDomene(): List<Segment<Unntak>> {
    return this.map { it.tilDomene() }
}

data class KildesystemMedl(
    val kildesystemKode: KildesystemKode,
    val kildeNavn: String
)

enum class KildesystemKode {
    APPBRK,
    AVGSYS,
    E500,
    INFOTR,
    LAANEKASSEN,
    MEDL,
    PP01,
    srvgosys,
    srvmelosys,
    TP,
    TPS,
}