package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.komponenter.gateway.Gateway

interface TjenestePensjonGateway : Gateway {
    fun hentTjenestePensjon(ident: String): List<TjenestePensjonForhold>
}