package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

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
        TilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
        TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
        TilbakekrevingBehandlingsstatus.TIL_BESLUTTER -> no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
    }
}