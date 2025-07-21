package no.nav.aap.behandlingsflyt.exception

import no.nav.aap.komponenter.httpklient.exception.ErrorRespons
import no.nav.aap.komponenter.httpklient.exception.GenerellErrorRespons

class BehandlingUnderProsesseringException(typer: List<String>) : FlytOperasjonException,
    RuntimeException("Behandlingen har prosesseringsjobber som venter eller har feilet. Vent til disse er ferdig prosesserte. Jobbtyper: $typer") {

    override fun body(): ErrorRespons {
        return GenerellErrorRespons(message)
    }
}