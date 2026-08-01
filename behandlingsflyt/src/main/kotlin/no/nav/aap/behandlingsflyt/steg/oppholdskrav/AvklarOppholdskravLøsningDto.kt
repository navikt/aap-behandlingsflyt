package no.nav.aap.behandlingsflyt.steg.oppholdskrav

import java.time.LocalDate
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.oppholdskrav.OppholdskravPeriode

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