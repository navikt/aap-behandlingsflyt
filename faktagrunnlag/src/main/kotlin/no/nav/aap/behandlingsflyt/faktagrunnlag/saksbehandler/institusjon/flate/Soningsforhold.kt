package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import java.time.LocalDate

data class Soningsforhold(
    val vurderingsdato: LocalDate,
    val info: InstitusjonsoppholdDto,
    val vurdering: Soningsvurdering?,
    val status: OppholdVurdering
)