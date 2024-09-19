package no.nav.aap.behandlingsflyt.kontrakt.steg

import no.nav.aap.behandlingsflyt.kontrakt.sak.Status

enum class StegType(
    val gruppe: StegGruppe,
    val status: Status,
    val tekniskSteg: Boolean = false
) {
    START_BEHANDLING(
        gruppe = StegGruppe.START_BEHANDLING,
        status = Status.OPPRETTET
    ),
    VURDER_ALDER(
        gruppe = StegGruppe.ALDER,
        status = Status.UTREDES,
    ),
    VURDER_LOVVALG(
        gruppe = StegGruppe.LOVVALG,
        status = Status.UTREDES
    ),
    VURDER_MEDLEMSKAP(
        gruppe = StegGruppe.MEDLEMSKAP,
        status = Status.UTREDES
    ),
    AVKLAR_STUDENT(
        gruppe = StegGruppe.STUDENT,
        status = Status.UTREDES
    ),
    VURDER_BISTANDSBEHOV(
        gruppe = StegGruppe.SYKDOM,
        status = Status.UTREDES
    ),
    VURDER_SYKEPENGEERSTATNING(
        gruppe = StegGruppe.SYKDOM,
        status = Status.UTREDES
    ),
    FRITAK_MELDEPLIKT(
        gruppe = StegGruppe.SYKDOM,
        status = Status.UTREDES
    ),
    KVALITETSSIKRING(
        gruppe = StegGruppe.KVALITETSSIKRING,
        status = Status.UTREDES,
    ),
    BARNETILLEGG(
        gruppe = StegGruppe.BARNETILLEGG,
        status = Status.UTREDES
    ),
    AVKLAR_SYKDOM(
        gruppe = StegGruppe.SYKDOM,
        status = Status.UTREDES
    ),
    FASTSETT_ARBEIDSEVNE(
        gruppe = StegGruppe.SYKDOM,
        status = Status.UTREDES
    ),
    FASTSETT_BEREGNINGSTIDSPUNKT(
        gruppe = StegGruppe.GRUNNLAG,
        status = Status.UTREDES
    ),
    FASTSETT_GRUNNLAG(
        gruppe = StegGruppe.GRUNNLAG,
        status = Status.UTREDES
    ),
    VIS_GRUNNLAG(
        gruppe = StegGruppe.GRUNNLAG,
        status = Status.UTREDES
    ),
    FASTSETT_UTTAK(
        gruppe = StegGruppe.UNDERVEIS,
        status = Status.UTREDES
    ),
    SAMORDNING_GRADERING(
        gruppe = StegGruppe.UNDERVEIS,
        status = Status.UTREDES
    ),
    DU_ER_ET_ANNET_STED(
        gruppe = StegGruppe.ET_ANNET_STED,
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
        gruppe = StegGruppe.FATTE_VEDTAK,
        status = Status.UTREDES
    ),
    BREV(
        gruppe = StegGruppe.BREV,
        status = Status.AVSLUTTET,
    ),
    IVERKSETT_VEDTAK(
        gruppe = StegGruppe.IVERKSETT_VEDTAK,
        status = Status.AVSLUTTET
    ),
    UDEFINERT(
        gruppe = StegGruppe.UDEFINERT,
        status = Status.UTREDES,
        tekniskSteg = true,
    ) // Forbeholdt deklarasjon for avklaringsbehov som
}