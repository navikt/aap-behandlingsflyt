package no.nav.aap.behandlingsflyt.behandling.avslag11_27

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.VurderingForKravGrunnlag

class Avslag11_27Grunnlag(
    override val vurderinger: Set<Avslag11_27Vurdering>
) : VurderingForKravGrunnlag<Avslag11_27Vurdering>