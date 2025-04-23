package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.SamhandlerForholdDto
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.gateway.Gateway

interface TjenestePensjonGateway : Gateway {
    fun hentTjenestePensjon(ident: String, periode: Periode): List<SamhandlerForholdDto>
}