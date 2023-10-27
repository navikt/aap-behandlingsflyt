package no.nav.aap.behandlingsflyt.sak.flate

import com.fasterxml.jackson.annotation.JsonProperty

data class FinnSakForIdentDTO(@JsonProperty(value = "ident", required = true) val ident: String)