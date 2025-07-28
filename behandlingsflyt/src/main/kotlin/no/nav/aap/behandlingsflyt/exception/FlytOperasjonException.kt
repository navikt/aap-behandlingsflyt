package no.nav.aap.behandlingsflyt.exception

import no.nav.aap.komponenter.httpklient.exception.ErrorRespons

sealed interface FlytOperasjonException {

    fun body(): ErrorRespons
}