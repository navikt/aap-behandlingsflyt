package no.nav.aap.behandlingsflyt.exception

import io.ktor.http.*
import no.nav.aap.behandlingsflyt.ErrorRespons

interface FlytOperasjonException {
    fun status(): HttpStatusCode

    fun body(): ErrorRespons
}