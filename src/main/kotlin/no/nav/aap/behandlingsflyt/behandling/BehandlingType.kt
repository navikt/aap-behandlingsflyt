package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Anke
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Klage
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Revurdering
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Tilbakekreving

interface BehandlingType {
    fun flyt(): BehandlingFlyt
    fun identifikator(): String
}

fun utledType(identifikator: String): BehandlingType {
    if (Førstegangsbehandling.identifikator() == identifikator) {
        return Førstegangsbehandling
    }
    if (Revurdering.identifikator() == identifikator) {
        return Revurdering
    }
    if (Tilbakekreving.identifikator() == identifikator) {
        return Tilbakekreving
    }
    if (Klage.identifikator() == identifikator) {
        return Klage
    }
    if (Anke.identifikator() == identifikator) {
        return Anke
    }
    throw IllegalArgumentException("Ukjent identifikator ${identifikator}")
}

