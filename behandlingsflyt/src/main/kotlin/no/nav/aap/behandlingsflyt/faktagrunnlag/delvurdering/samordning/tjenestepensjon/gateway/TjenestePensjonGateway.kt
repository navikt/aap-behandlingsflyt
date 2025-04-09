package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRequest
import no.nav.aap.lookup.gateway.Gateway

interface TjenestePensjonGateway : Gateway {
    fun hentTjenestePensjon(request: TjenestePensjonRequest): TjenestePensjon
}