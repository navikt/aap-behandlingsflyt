package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter

import java.time.LocalDate

// TODO: slett denne når integrasjon med yrkesskade er på plass
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