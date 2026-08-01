package no.nav.aap.misc

import no.nav.aap.komponenter.json.DefaultJsonMapper

private val mapper = DefaultJsonMapper.objectMapper()

interface Faktagrunnlag {
    fun hent(): String? {
        return mapper.writeValueAsString(this)
    }
}