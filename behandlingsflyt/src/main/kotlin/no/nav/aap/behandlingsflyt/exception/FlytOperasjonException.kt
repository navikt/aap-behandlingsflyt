package no.nav.aap.behandlingsflyt.exception

import io.ktor.http.*

interface FlytOperasjonException {
    fun status(): HttpStatusCode

    fun body(): ErrorRespons
}