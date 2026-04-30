package no.nav.aap.behandlingsflyt.integrasjon.yrkesskade

import java.time.LocalDate

data class YrkesskadeModell(
    val kommunenr: String,
    val saksblokk: String,
    // bruke denne?
    val saksnr: Int,
    val sakstype: String,
    val mottattdato: LocalDate,
    val resultat: String,
    val resultattekst: String,
    val vedtaksdato: LocalDate?,
    val skadeart: String,
    val diagnose: String,
    val skadedato: LocalDate?,
    val kildetabell: String,
    val kildesystem: String,
    val saksreferanse: String,
    val eksternreferanser: List<String>?,
    val skadekombinasjoner: List<SkadekombinasjonModell>?,
    val skadekombinasjonerTekst: String?,
    val saksbehandlingsansvarligIdent: String?
)

data class SkadekombinasjonModell(
    val kroppsdel: String,
    val skadetype: String,
)