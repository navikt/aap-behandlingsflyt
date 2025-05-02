package no.nav.aap.behandlingsflyt.exception

import io.ktor.http.*
import no.nav.aap.komponenter.httpklient.exception.ErrorRespons

interface FlytOperasjonException {
    fun status(): HttpStatusCode

    fun body(): ErrorRespons
}