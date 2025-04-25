package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.type.Periode

interface TjenestePensjonGateway : Gateway {
    fun hentTjenestePensjon(ident: String, periode: Periode): List<TjenestePensjonForhold>
}