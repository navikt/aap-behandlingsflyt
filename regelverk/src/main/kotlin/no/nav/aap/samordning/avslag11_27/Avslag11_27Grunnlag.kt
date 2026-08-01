package no.nav.aap.samordning.avslag11_27

import no.nav.aap.misc.VurderingForKravGrunnlag

class Avslag11_27Grunnlag(
    override val vurderinger: Set<Avslag11_27Vurdering>
) : VurderingForKravGrunnlag<Avslag11_27Vurdering>