package no.nav.aap.behandlingsflyt.integrasjon.yrkesskade

import java.time.LocalDate

class YrkesskadeModell(
    val kommunenr: String,
    val saksblokk: String,
    // bruke denne?
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