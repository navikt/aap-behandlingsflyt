package no.nav.aap.yrkesskade

import java.time.LocalDate

data class YrkesskadeRequest(
    val foedselsnumre: List<String>,
    val fomDato: LocalDate
)

data class YrkesskadeModell (
    val kommunenr: String,
    val saksblokk: String,
    val saksnr: Int,
    val sakstype: String,
    val mottattdato: LocalDate,
    val resultat: String,
    val resultattekst: String,
    val vedtaksdato: LocalDate,
    val skadeart: String,
    val diagnose: String,
    val skadedato: LocalDate,
    val kildetabell: String,
    val kildesystem: String,
    val saksreferanse: String
)

data class Yrkesskader (
    val skader:List<YrkesskadeModell>?
)
