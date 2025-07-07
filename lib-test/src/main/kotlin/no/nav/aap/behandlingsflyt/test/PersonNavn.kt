package no.nav.aap.behandlingsflyt.test

import com.fasterxml.jackson.annotation.JsonProperty


class PersonNavn(
    @param:JsonProperty("fornavn") val fornavn: String,
    @param:JsonProperty("etternavn") val etternavn: String
) {

    override fun toString(): String {
        return "PersonNavn(fornavn='$fornavn', etternavn='$etternavn')"
    }
}
