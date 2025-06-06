package no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.adapter.EnhetsregisterOrganisasjonRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.adapter.EnhetsregisterOrganisasjonResponse
import no.nav.aap.komponenter.gateway.Gateway

interface EnhetsregisteretGateway : Gateway {
    fun hentEREGData(request: EnhetsregisterOrganisasjonRequest): EnhetsregisterOrganisasjonResponse?
}