package no.nav.aap.behandlingsflyt.behandling.barnetillegg.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnetilleggDto(
    val oppgitteBarn: List<Ident>,
    val folkeregisterbarn: List<IdentifiserteBarnDto>,
    val vurderteBarn: List<VurdertBarn>,
    val barnSomTrengerVurdering: List<IdentifiserteBarnDto>
)