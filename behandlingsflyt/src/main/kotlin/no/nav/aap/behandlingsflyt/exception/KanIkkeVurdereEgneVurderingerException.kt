package no.nav.aap.behandlingsflyt.exception

import no.nav.aap.komponenter.httpklient.exception.ErrorRespons
import no.nav.aap.komponenter.httpklient.exception.GenerellErrorRespons

class KanIkkeVurdereEgneVurderingerException : FlytOperasjonException,
    RuntimeException("Kan ikke vurdere kvalitet/to-trinn på egne vurderinger.") {

    override fun body(): ErrorRespons {
        return GenerellErrorRespons(message)
    }
}