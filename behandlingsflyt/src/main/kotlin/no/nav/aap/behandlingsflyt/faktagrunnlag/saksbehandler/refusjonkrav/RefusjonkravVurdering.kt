package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav

import java.time.LocalDate
import java.time.LocalDateTime

data class RefusjonkravVurdering(
    val harKrav: Boolean,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val navKontor: String?,
    val vurdertAv: String,
    val opprettetTid: LocalDateTime? = null,
) {
    fun tilNavKontorPeriodeDto( virkningsdato : LocalDate,vedtaksdato: LocalDate): NavKontorPeriodeDto {
        return NavKontorPeriodeDto(
            enhetsNummer = navKontorEnhetsNummer(navKontor) ?: "Kunne ikke utlede navkontor enhetsnummer basert på $navKontor",
            virkingsdato = virkningsdato,
            vedtaksdato = vedtaksdato
        )
    }
    fun tilNavKontorPeriodeDto( ): NavKontorPeriodeDto {
        return NavKontorPeriodeDto(
            enhetsNummer = navKontorEnhetsNummer(navKontor) ?: "Kunne ikke utlede navkontor enhetsnummer basert på $navKontor",
            virkingsdato = fom,
            vedtaksdato = tom
        )
    }


}

data class RefusjonkravVurderingDto(
    val harKrav: Boolean,
    val navKontor: String?,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)

data class NavKontorPeriodeDto(
    val enhetsNummer: String,
    val virkingsdato: LocalDate?,
    val vedtaksdato: LocalDate?,
)

fun navKontorEnhetsNummer(input: String?): String? {
    return input?.substringAfterLast(" - ")
}
