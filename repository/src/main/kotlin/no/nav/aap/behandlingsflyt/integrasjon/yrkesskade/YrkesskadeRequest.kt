package no.nav.aap.behandlingsflyt.integrasjon.yrkesskade

import java.time.LocalDate

class YrkesskadeRequest(
    val foedselsnumre: List<String>,
    val fomDato: LocalDate
)