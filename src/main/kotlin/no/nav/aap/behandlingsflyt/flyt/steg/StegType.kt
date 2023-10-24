package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.domene.behandling.Status

enum class StegType(val gruppe: StegGruppe, val status: Status, val tekniskSteg: Boolean = false) {
    START_BEHANDLING(
        gruppe = StegGruppe.START_BEHANDLING,
        status = Status.OPPRETTET
    ),
    VURDER_ALDER(gruppe = StegGruppe.ALDER, status = Status.UTREDES),
    VURDER_LOVVALG(
        gruppe = StegGruppe.LOVVALG,
        status = Status.UTREDES
    ),
    VURDER_MEDLEMSKAP(
        gruppe = StegGruppe.MEDLEMSKAP,
        status = Status.UTREDES
    ),
    AVKLAR_STUDENT(
        gruppe = StegGruppe.SYKDOM,
        status = Status.UTREDES
    ),
    VURDER_BISTANDSBEHOV(
        gruppe = StegGruppe.SYKDOM,
        status = Status.UTREDES
    ),
    VURDER_SYKEPENGEERSTATNING(
        gruppe = StegGruppe.SYKEPENGEERSTATNING,
        status = Status.UTREDES
    ),
    FRITAK_MELDEPLIKT(
        gruppe = StegGruppe.SYKDOM,
        status = Status.UTREDES
    ),
    BARNETILLEGG(gruppe = StegGruppe.BARNETILLEGG, status = Status.UTREDES),
    SAMORDNING(
        gruppe = StegGruppe.SAMORDNING,
        status = Status.UTREDES
    ),
    AVKLAR_YRKESSKADE(gruppe = StegGruppe.SYKDOM, status = Status.UTREDES),
    AVKLAR_SYKDOM(
        gruppe = StegGruppe.SYKDOM,
        status = Status.UTREDES
    ),
    FASTSETT_GRUNNLAG(gruppe = StegGruppe.GRUNNLAG, status = Status.UTREDES),
    FASTSETT_UTTAK(
        gruppe = StegGruppe.UTTAK,
        status = Status.UTREDES
    ),
    BEREGN_TILKJENT_YTELSE(
        gruppe = StegGruppe.TILKJENT_YTELSE,
        status = Status.UTREDES
    ),
    SIMULERING(
        gruppe = StegGruppe.SIMULERING,
        status = Status.UTREDES
    ),
    FORESLÅ_VEDTAK(
        gruppe = StegGruppe.VEDTAK,
        status = Status.UTREDES
    ),
    FATTE_VEDTAK(
        gruppe = StegGruppe.VEDTAK,
        status = Status.UTREDES
    ),
    IVERKSETT_VEDTAK(gruppe = StegGruppe.VEDTAK, status = Status.AVSLUTTET),
    UDEFINERT(
        gruppe = StegGruppe.UDEFINERT, status = Status.UTREDES, tekniskSteg = true
    ), // Forbeholdt deklarasjon for avklaringsbehov som

}
