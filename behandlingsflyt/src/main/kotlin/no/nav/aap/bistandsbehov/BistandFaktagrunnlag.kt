package no.nav.aap.bistandsbehov

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import java.time.LocalDate

class BistandFaktagrunnlag(
    val sisteDagMedMuligYtelse: LocalDate,
    val bistandGrunnlag: BistandGrunnlag?,
) : Faktagrunnlag
