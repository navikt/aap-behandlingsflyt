package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav

import java.time.LocalDate
import java.time.LocalDateTime

data class RefusjonkravVurdering(
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val navKontor: String?,
    val vurdertAv: String,
    val opprettetTid: LocalDateTime? = null,
) {
    fun tilNavKontorPeriodeDto(): NavKontorPeriodeDto {
        return NavKontorPeriodeDto(
            enhetsNummer = navKontorEnhetsNummer(navKontor) ?: "Kunne ikke utlede navkontor enhetsnummer basert på $navKontor",
            fom = fom,
            tom = tom
        )
    }
}

data class RefusjonkravVurderingDto(
    val harKrav: Boolean,
    val navKontor: String?,
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class NavKontorPeriodeDto(
    val enhetsNummer: String,
    val fom: LocalDate?,
    val tom: LocalDate?,
)

fun navKontorEnhetsNummer(input: String?): String? {
    return input?.substringAfterLast(" - ")
}
