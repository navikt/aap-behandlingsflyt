package no.nav.aap.behandlingsflyt.flyt.flate

import com.fasterxml.jackson.annotation.JsonProperty

data class TaAvVentRequest(@JsonProperty(
    value = "behandlingVersjon",
    required = true,
    defaultValue = "0"
) val behandlingVersjon: Long,
   val begrunnelse: String
)