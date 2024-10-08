package no.nav.aap.behandlingsflyt.behandling.barnetillegg.flate

import java.time.LocalDate

class BarnetilleggDto(
    val søknadstidspunkt: LocalDate,
    val folkeregisterbarn: List<IdentifiserteBarnDto>,
    val vurderteBarn: List<ExtendedVurdertBarnDto>,
    val barnSomTrengerVurdering: List<IdentifiserteBarnDto>
)