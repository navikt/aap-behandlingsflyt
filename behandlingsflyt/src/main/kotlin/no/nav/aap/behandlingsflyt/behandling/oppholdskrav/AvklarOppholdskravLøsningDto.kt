package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import java.time.LocalDate

data class AvklarOppholdkravLøsningForPeriodeDto(
    override val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate? = null,
    val oppfylt: Boolean,
    val land: String?,
): LøsningForPeriode {
    fun tilOppholdskravPeriode() = OppholdskravPeriode(
        fom = fom,
        tom = tom,
        begrunnelse = begrunnelse,
        land = land,
        oppfylt = oppfylt,
    )
}


