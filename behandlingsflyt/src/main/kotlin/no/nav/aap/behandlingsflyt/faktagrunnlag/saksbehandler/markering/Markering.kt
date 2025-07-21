package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.markering

import no.nav.aap.behandlingsflyt.kontrakt.behandling.MarkeringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

data class Markering(
    val forBehandling: BehandlingId,
    val markeringType: MarkeringType,
    val begrunnelse: String,
    val erAktiv: Boolean,
    val opprettetAv: String
)