package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering

data class HelseOpphold(val vurdering: OppholdVurdering, val umiddelbarReduksjon: Boolean = false)
