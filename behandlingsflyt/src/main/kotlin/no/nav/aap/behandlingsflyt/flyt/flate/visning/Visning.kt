package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling

data class Visning(
    val saksbehandlerReadOnly: Boolean,
    val beslutterReadOnly: Boolean,
    val visBeslutterKort: Boolean,
    val visVentekort: Boolean,
    val kvalitetssikringReadOnly: Boolean,
    val visKvalitetssikringKort: Boolean,
    val typeBehandling: TypeBehandling
)
