package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import io.ktor.http.*
import no.nav.aap.behandlingsflyt.exception.FlytOperasjonException
import no.nav.aap.komponenter.httpklient.exception.ErrorRespons
import no.nav.aap.komponenter.httpklient.exception.GenerellErrorRespons

class OutdatedBehandlingException(årsak: String) : FlytOperasjonException, RuntimeException(årsak) {
    override fun status(): HttpStatusCode {
        return HttpStatusCode.Conflict
    }

    override fun body(): ErrorRespons {
        return GenerellErrorRespons(message)
    }
}
