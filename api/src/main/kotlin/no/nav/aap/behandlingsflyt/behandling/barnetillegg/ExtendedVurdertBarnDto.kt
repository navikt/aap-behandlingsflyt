package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import java.time.LocalDate

class ExtendedVurdertBarnDto(
    ident: String,
    vurderinger: List<VurderingAvForeldreAnsvar>,
    val fødselsdato: LocalDate,
) : VurdertBarnDto(ident, vurderinger)