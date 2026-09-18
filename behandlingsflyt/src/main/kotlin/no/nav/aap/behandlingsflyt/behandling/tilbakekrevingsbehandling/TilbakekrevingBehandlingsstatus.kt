package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus as KontraktTilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status

enum class TilbakekrevingBehandlingsstatus {
    OPPRETTET,
    TIL_FORHÅNDSVARSEL,
    TIL_BEHANDLING,
    RETUR_FRA_BESLUTTER,
    TIL_GODKJENNING,
    TIL_BESLUTTER,
    AVSLUTTET,
}

fun TilbakekrevingBehandlingsstatus.tilKontrakt(): KontraktTilbakekrevingBehandlingsstatus {
    return when(this){
        TilbakekrevingBehandlingsstatus.AVSLUTTET -> KontraktTilbakekrevingBehandlingsstatus.AVSLUTTET
        TilbakekrevingBehandlingsstatus.OPPRETTET -> KontraktTilbakekrevingBehandlingsstatus.OPPRETTET
        TilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER -> KontraktTilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER
        TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> KontraktTilbakekrevingBehandlingsstatus.TIL_BEHANDLING
        TilbakekrevingBehandlingsstatus.TIL_GODKJENNING -> KontraktTilbakekrevingBehandlingsstatus.TIL_GODKJENNING
        TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL -> KontraktTilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL
        TilbakekrevingBehandlingsstatus.TIL_BESLUTTER -> KontraktTilbakekrevingBehandlingsstatus.TIL_BESLUTTER
    }
}

fun TilbakekrevingBehandlingsstatus.tilBehandlingStatus(): Status {
    return when(this){
        TilbakekrevingBehandlingsstatus.OPPRETTET -> Status.OPPRETTET
        TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL -> Status.UTREDES
        TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> Status.UTREDES
        TilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER -> Status.UTREDES
        TilbakekrevingBehandlingsstatus.TIL_GODKJENNING -> Status.UTREDES
        TilbakekrevingBehandlingsstatus.TIL_BESLUTTER -> Status.UTREDES
        TilbakekrevingBehandlingsstatus.AVSLUTTET -> Status.AVSLUTTET
    }
}

fun TilbakekrevingBehandlingsstatus.erAvsluttet(): Boolean {
    return this == TilbakekrevingBehandlingsstatus.AVSLUTTET
}

