package no.nav.aap.behandlingsflyt.flyt.flate

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

class SettPåVentRequest(
    @JsonProperty(
        value = "behandlingVersjon",
        required = true,
        defaultValue = "0"
    ) val behandlingVersjon: Long, val begrunnelse: String, val frist: LocalDate?
)