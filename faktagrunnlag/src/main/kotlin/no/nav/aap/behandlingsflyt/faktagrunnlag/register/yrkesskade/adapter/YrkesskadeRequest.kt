package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter

import java.time.LocalDate

class YrkesskadeRequest(
    val foedselsnumre: List<String>,
    val fomDato: LocalDate
)