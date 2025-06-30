package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import java.time.LocalDate

data class BarnetilleggDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val søknadstidspunkt: LocalDate,
    val folkeregisterbarn: List<IdentifiserteBarnDto>,
    val vurderteBarn: List<ExtendedVurdertBarnDto>,
    val vurdertAv: VurdertAvResponse?,
    val barnSomTrengerVurdering: List<IdentifiserteBarnDto>
)