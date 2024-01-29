package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Anke
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Klage
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Revurdering
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Tilbakekreving
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling

interface BehandlingType {
    fun flyt(): BehandlingFlyt
}

fun utledType(identifikator: TypeBehandling): BehandlingType {
    return when (identifikator) {
        TypeBehandling.Førstegangsbehandling -> Førstegangsbehandling
        TypeBehandling.Revurdering -> Revurdering
        TypeBehandling.Tilbakekreving -> Tilbakekreving
        TypeBehandling.Klage -> Klage
        TypeBehandling.Anke -> Anke
    }
}