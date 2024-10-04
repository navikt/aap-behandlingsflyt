package no.nav.aap.behandlingsflyt.behandling.barnetillegg.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto

class BarnetilleggDto(
    val folkeregisterbarn: List<IdentifiserteBarnDto>,
    val vurderteBarn: List<VurdertBarnDto>,
    val barnSomTrengerVurdering: List<IdentifiserteBarnDto>
)