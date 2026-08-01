package no.nav.aap.oppholdskrav

import java.time.LocalDate
import no.nav.aap.misc.Faktagrunnlag

data class OppholdskravvilkårGrunnlag(
    val oppholdskravGrunnlag: OppholdskravGrunnlag?,
    val vurderFra: LocalDate,
) : Faktagrunnlag