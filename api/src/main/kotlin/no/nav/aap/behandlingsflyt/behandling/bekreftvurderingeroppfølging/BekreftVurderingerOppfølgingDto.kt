package no.nav.aap.behandlingsflyt.behandling.bekreftvurderingeroppfølging

import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurdering

data class BekreftVurderingerOppfølgingDto(
    val mellomlagredeVurderinger: List<MellomlagretVurdering> 
)