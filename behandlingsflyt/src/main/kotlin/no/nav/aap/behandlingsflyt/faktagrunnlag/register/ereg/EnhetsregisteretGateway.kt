package no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.adapter.EnhetsregisterOrganisasjonResponse
import no.nav.aap.komponenter.gateway.Gateway

@JvmInline
value class Organisasjonsnummer(val value: String)

interface EnhetsregisteretGateway : Gateway {
    fun hentEREGData(organisasjonsnummer: Organisasjonsnummer): EnhetsregisterOrganisasjonResponse?
}