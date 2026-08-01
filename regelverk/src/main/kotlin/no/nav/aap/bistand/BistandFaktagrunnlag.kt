package no.nav.aap.bistand

import java.time.LocalDate
import no.nav.aap.misc.Faktagrunnlag

class BistandFaktagrunnlag(
    val sisteDagMedMuligYtelse: LocalDate,
    val bistandGrunnlag: BistandGrunnlag?,
) : Faktagrunnlag