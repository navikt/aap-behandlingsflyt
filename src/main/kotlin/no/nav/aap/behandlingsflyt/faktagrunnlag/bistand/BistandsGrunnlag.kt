package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand

import no.nav.aap.behandlingsflyt.avklaringsbehov.bistand.BistandsVurdering

class BistandsGrunnlag(
    val id: Long,
    val behandlingId: Long,
    val vurdering: BistandsVurdering?,
)