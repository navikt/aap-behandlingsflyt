package no.nav.aap.behandlingsflyt.steg.samordning.tjenestepensjon.gateway

import no.nav.aap.behandlingsflyt.steg.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.komponenter.gateway.Gateway

interface TjenestePensjonGateway : Gateway {
    fun hentTjenestePensjon(ident: String): List<TjenestePensjonForhold>
}