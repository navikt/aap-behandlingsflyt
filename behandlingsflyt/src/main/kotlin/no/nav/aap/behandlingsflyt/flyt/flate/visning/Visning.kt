package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling

data class Visning(
    val saksbehandlerReadOnly: Boolean,
    val beslutterReadOnly: Boolean,
    val brukerHarKvalitetssikret: Boolean,
    val brukerHarBesluttet: Boolean,
    val kvalitetssikringReadOnly: Boolean,
    val visBeslutterKort: Boolean,
    val visVentekort: Boolean,
    val visBrevkort: Boolean,
    val visKvalitetssikringKort: Boolean,
    val typeBehandling: TypeBehandling
)
