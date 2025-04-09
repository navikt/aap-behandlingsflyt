package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import no.nav.aap.komponenter.type.Periode

data class TjenestePensjonRequest(
    val fnr: String,
    val periode: Periode
)

data class TjenestePensjon(
    val tp: List<String>
)