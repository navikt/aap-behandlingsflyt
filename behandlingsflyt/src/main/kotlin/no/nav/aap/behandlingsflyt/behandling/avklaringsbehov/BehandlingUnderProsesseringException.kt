package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import io.ktor.http.*
import no.nav.aap.behandlingsflyt.exception.FlytOperasjonException
import no.nav.aap.komponenter.httpklient.exception.ErrorRespons
import no.nav.aap.komponenter.httpklient.exception.GenerellErrorRespons

class BehandlingUnderProsesseringException : FlytOperasjonException,
    RuntimeException("Behandlingen har prosesseringsjobber som venter eller har feilet. Vent til disse er ferdig prosesserte") {
    override fun status(): HttpStatusCode {
        return HttpStatusCode.Conflict
    }

    override fun body(): ErrorRespons {
        return GenerellErrorRespons(message)
    }
}
