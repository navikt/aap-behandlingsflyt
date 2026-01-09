package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.komponenter.json.DefaultJsonMapper

private val mapper = DefaultJsonMapper.objectMapper()

interface Faktagrunnlag {
    fun hent(): String? {
        return mapper.writeValueAsString(this)
    }
}