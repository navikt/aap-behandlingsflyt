package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import io.ktor.http.*
import no.nav.aap.behandlingsflyt.exception.ErrorRespons
import no.nav.aap.behandlingsflyt.exception.FlytOperasjonException

class KanIkkeVurdereEgneVurderingerException : FlytOperasjonException,
    RuntimeException("Kan ikke vurdere kvalitet/to-trinn på egne vurderinger") {
    override fun status(): HttpStatusCode {
        return HttpStatusCode.Forbidden
    }

    override fun body(): ErrorRespons {
        return ErrorRespons(cause?.message)
    }
}
