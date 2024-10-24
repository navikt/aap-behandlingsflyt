package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.komponenter.type.Periode

data class Helseopphold(
    val periode: Periode,
    val vurderinger: List<HelseinstitusjonVurdering>?,
    val status: OppholdVurdering
)