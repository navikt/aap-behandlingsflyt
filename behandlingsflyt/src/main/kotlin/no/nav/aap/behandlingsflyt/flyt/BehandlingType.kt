package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Klage
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Revurdering
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Tilbakekreving
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling

interface BehandlingType {
    fun flyt(): BehandlingFlyt
}

fun TypeBehandling.flyt(): BehandlingFlyt = when (this) {
    TypeBehandling.Førstegangsbehandling -> Førstegangsbehandling.flyt()
    TypeBehandling.Revurdering -> Revurdering.flyt()
    TypeBehandling.Tilbakekreving -> Tilbakekreving.flyt()
    TypeBehandling.Klage -> Klage.flyt()
}