package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import java.time.LocalDate

data class AvklarOppholdskravLøsningDto(
    val perioder: List<AvklarOppholdkravLøsningPeriodeDto>,
)

data class AvklarOppholdkravLøsningPeriodeDto(
    val oppfylt: Boolean,
    val begrunnelse: String,
    val land: String?,
    val fom: LocalDate,
    val tom: LocalDate? = null,
){
    fun tilOppholdskravPeriode() = OppholdskravPeriode(
        fom = fom,
        tom = tom,
        begrunnelse = begrunnelse,
        land = land,
        oppfylt = oppfylt,
    )
}


