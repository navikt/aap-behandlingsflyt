package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import no.nav.aap.lookup.gateway.Gateway

interface ForeldrepengerGateway : Gateway {
    fun hentVedtakYtelseForPerson(request: ForeldrepengerRequest): ForeldrepengerResponse
}