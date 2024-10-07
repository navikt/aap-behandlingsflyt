package no.nav.aap.behandlingsflyt.behandling.barnetillegg.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import java.time.LocalDate

class BarnetilleggDto(
    val søknadstidspunkt: LocalDate,
    val folkeregisterbarn: List<IdentifiserteBarnDto>,
    val vurderteBarn: List<VurdertBarnDto>,
    val barnSomTrengerVurdering: List<IdentifiserteBarnDto>
)