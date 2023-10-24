package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.SykepengerVurdering

class SykepengerErstatningGrunnlag(
    val id: Long,
    val behandlingId: Long,
    val vurdering: SykepengerVurdering?
)
