package no.nav.aap.behandlingsflyt.avklaringsbehov

import io.ktor.http.*
import no.nav.aap.behandlingsflyt.ErrorRespons
import no.nav.aap.behandlingsflyt.exception.FlytOperasjonException

class BehandlingUnderProsesseringException() : FlytOperasjonException,
    RuntimeException("Behandlingen har prosesseringsoppgaver som venter eller har feilet. Vent til disse er ferdig prosesserte") {
    override fun status(): HttpStatusCode {
        return HttpStatusCode.Conflict
    }

    override fun body(): ErrorRespons {
        return ErrorRespons(cause?.message)
    }
}
