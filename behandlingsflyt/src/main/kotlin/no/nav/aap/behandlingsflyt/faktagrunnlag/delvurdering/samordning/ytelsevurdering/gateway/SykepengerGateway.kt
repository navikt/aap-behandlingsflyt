package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import no.nav.aap.komponenter.gateway.Gateway

interface SykepengerGateway: Gateway {
    fun hentYtelseSykepenger(request: SykepengerRequest): SykepengerResponse
}