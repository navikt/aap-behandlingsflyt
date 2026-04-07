package no.nav.aap.behandlingsflyt.test

import com.fasterxml.jackson.annotation.JsonProperty


data class PersonNavn(
    @param:JsonProperty("fornavn") val fornavn: String,
    @param:JsonProperty("etternavn") val etternavn: String
)
