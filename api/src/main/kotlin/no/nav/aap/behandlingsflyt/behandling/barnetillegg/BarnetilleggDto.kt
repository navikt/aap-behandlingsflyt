package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import java.time.LocalDate

data class BarnetilleggDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val søknadstidspunkt: LocalDate,
    val barnSomTrengerVurdering: List<IdentifiserteBarnDto>,
    val folkeregisterbarn: List<IdentifiserteBarnDto>,
    val saksbehandlerOppgitteBarn: List<IdentifiserteBarnDto>,
    val vurderteBarn: List<ExtendedVurdertBarnDto>,
    val vurderteFolkeregisterBarn: List<ExtendedVurdertBarnDto>,
    val vurderteSaksbehandlerOppgitteBarn: List<SlettbarVurdertBarnDto>,
    val vurdertAv: VurdertAvResponse?
)