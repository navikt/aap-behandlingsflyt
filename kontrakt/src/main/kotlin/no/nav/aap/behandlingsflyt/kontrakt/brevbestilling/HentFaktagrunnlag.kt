package no.nav.aap.behandlingsflyt.kontrakt.brevbestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse

public data class HentFaktaGrunnlagRequest(
    val behandlingReferanse: BehandlingReferanse,
    val faktagrunnlag: Set<FaktagrunnlagType>
)
