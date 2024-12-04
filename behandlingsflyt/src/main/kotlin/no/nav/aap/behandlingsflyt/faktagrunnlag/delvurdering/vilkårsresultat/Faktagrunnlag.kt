package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

private val mapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

interface Faktagrunnlag {
    fun hent(): String? {
        return mapper.writeValueAsString(this)
    }
}