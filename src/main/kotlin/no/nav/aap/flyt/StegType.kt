package no.nav.aap.flyt

import no.nav.aap.domene.behandling.Status

enum class StegType(val status: Status, val tekniskSteg: Boolean = false) {
    START_BEHANDLING(status = Status.OPPRETTET),
    AVKLAR_YRKESSKADE(status = Status.UTREDES),
    INNHENT_REGISTERDATA(status = Status.UTREDES),
    INNGANGSVILKÅR(status = Status.UTREDES),
    FASTSETT_GRUNNLAG(status = Status.UTREDES),
    FASTSETT_UTTAK(status = Status.UTREDES),
    BEREGN_TILKJENT_YTELSE(status = Status.UTREDES),
    SIMULERING(status = Status.UTREDES),
    FORESLÅ_VEDTAK(status = Status.UTREDES),
    FATTE_VEDTAK(status = Status.UTREDES),
    IVERKSETT_VEDTAK(status = Status.IVERKSETTES),
    UDEFINERT(status = Status.UTREDES, tekniskSteg = true), // Forbeholdt deklarasjon for avklaringsbehov som
    AVSLUTT_BEHANDLING(status = Status.AVSLUTTET, tekniskSteg = true) // Forbeholdt enden av saksbehandlingsflyten, skal ikke implementeres
}
