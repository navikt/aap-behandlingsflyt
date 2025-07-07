package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.fasterxml.jackson.annotation.JsonProperty

data class FinnSakForIdentDTO(@param:JsonProperty(value = "ident", required = true) val ident: String)