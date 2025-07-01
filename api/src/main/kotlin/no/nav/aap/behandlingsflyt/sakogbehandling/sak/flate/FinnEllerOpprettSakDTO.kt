package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class FinnEllerOpprettSakDTO(
    @param:JsonProperty(value = "ident", required = true) val ident: String,
    @param:JsonProperty(value = "søknadsdato", required = true) val søknadsdato: LocalDate
)