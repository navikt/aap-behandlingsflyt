package no.nav.aap.behandlingsflyt.exception

import no.nav.aap.komponenter.httpklient.exception.ErrorRespons
import no.nav.aap.komponenter.httpklient.exception.GenerellErrorRespons

class OutdatedBehandlingException(årsak: String) : FlytOperasjonException, RuntimeException(årsak) {

    override fun body(): ErrorRespons {
        return GenerellErrorRespons(message)
    }
}