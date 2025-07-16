package no.nav.aap.behandlingsflyt.exception

import no.nav.aap.komponenter.httpklient.exception.ErrorRespons
import no.nav.aap.komponenter.httpklient.exception.GenerellErrorRespons

class BehandlingUnderProsesseringException : FlytOperasjonException,
    RuntimeException("Behandlingen har prosesseringsjobber som venter eller har feilet. Vent til disse er ferdig prosesserte") {

    override fun body(): ErrorRespons {
        return GenerellErrorRespons(message)
    }
}