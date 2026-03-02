package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode

enum class TilbakekrevingBehandlingsstatus {
    OPPRETTET,
    TIL_BEHANDLING,
    TIL_BESLUTTER,
    RETUR_FRA_BESLUTTER,
    AVSLUTTET,
}

fun TilbakekrevingBehandlingsstatus.tilKontrakt(): no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus {
    return when(this){
        TilbakekrevingBehandlingsstatus.AVSLUTTET -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.AVSLUTTET
        TilbakekrevingBehandlingsstatus.OPPRETTET -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.OPPRETTET
        TilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER
        TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
        TilbakekrevingBehandlingsstatus.TIL_BESLUTTER -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.TIL_BESLUTTER
    }
}

fun TilbakekrevingBehandlingsstatus.tilAvklaringsBehov(): String? {
    return when(this){
        TilbakekrevingBehandlingsstatus.AVSLUTTET -> null
        TilbakekrevingBehandlingsstatus.OPPRETTET,
        TilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER,
        TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> AvklaringsbehovKode.`9082`.name
        TilbakekrevingBehandlingsstatus.TIL_BESLUTTER -> AvklaringsbehovKode.`9083`.name
    }
}