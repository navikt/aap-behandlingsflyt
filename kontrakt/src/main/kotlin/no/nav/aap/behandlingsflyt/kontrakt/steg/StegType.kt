package no.nav.aap.behandlingsflyt.kontrakt.steg

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status

public enum class StegType(
    public val gruppe: StegGruppe,
    public val status: Status,
    public val tekniskSteg: Boolean = false
) {
    SEND_FORVALTNINGSMELDING(
        gruppe = StegGruppe.SEND_FORVALTNINGSMELDING,
        status = Status.OPPRETTET
    ),
    START_BEHANDLING(
        gruppe = StegGruppe.START_BEHANDLING,
        status = Status.OPPRETTET
    ),
    VURDER_RETTIGHETSPERIODE(
        gruppe = StegGruppe.RETTIGHETSPERIODE,
        status = Status.UTREDES,
    ),
    SØKNAD(
        gruppe = StegGruppe.SØKNAD,
        status = Status.UTREDES,
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
    FASTSETT_MELDEPERIODER(
        gruppe = StegGruppe.UDEFINERT,
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
    FASTSETT_SYKDOMSVILKÅRET(
        gruppe = StegGruppe.SYKDOM,
        status = Status.UTREDES
    ),
    VURDER_YRKESSKADE(
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
    REFUSJON_KRAV(
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
    MANGLENDE_LIGNING(
        gruppe = StegGruppe.GRUNNLAG,
        status = Status.UTREDES
    ),
    SAMORDNING_UFØRE(
        gruppe = StegGruppe.SAMORDNING,
        status = Status.UTREDES
    ),
    SAMORDNING_GRADERING(
        gruppe = StegGruppe.SAMORDNING,
        status = Status.UTREDES
    ),
    SAMORDNING_AVSLAG(
        gruppe = StegGruppe.SAMORDNING,
        status = Status.UTREDES,
    ),
    SAMORDNING_ANDRE_STATLIGE_YTELSER(
        gruppe = StegGruppe.SAMORDNING,
        status = Status.UTREDES
    ),
    SAMORDNING_TJENESTEPENSJON_REFUSJONSKRAV(
        gruppe = StegGruppe.SAMORDNING,
        status = Status.UTREDES
    ),
    FASTSETT_UTTAK(
        gruppe = StegGruppe.UNDERVEIS,
        status = Status.UTREDES
    ),
    EFFEKTUER_11_7(
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
    IVERKSETT_VEDTAK(
        gruppe = StegGruppe.IVERKSETT_VEDTAK,
        status = Status.IVERKSETTES
    ),
    OPPRETT_REVURDERING(
        gruppe = StegGruppe.IVERKSETT_VEDTAK,
        status = Status.IVERKSETTES
    ),
    BREV(
        gruppe = StegGruppe.BREV,
        status = Status.IVERKSETTES,
    ),
    PÅKLAGET_BEHANDLING(
        gruppe = StegGruppe.FORMKRAV,
        status = Status.UTREDES,
    ),
    FORMKRAV(
        gruppe = StegGruppe.FORMKRAV,
        status = Status.UTREDES,
    ),
    BEHANDLENDE_ENHET(
        gruppe = StegGruppe.FORMKRAV,
        status = Status.UTREDES,
    ),
    KLAGEBEHANDLING_KONTOR(
        gruppe = StegGruppe.KLAGEBEHANDLING_KONTOR,
        status = Status.UTREDES
    ),
    KLAGEBEHANDLING_NAY(
        gruppe = StegGruppe.KLAGEBEHANDLING_NAY,
        status = Status.UTREDES
    ),
    KLAGEBEHANDLING_OPPSUMMERING(
        gruppe = StegGruppe.KLAGEBEHANDLING_NAY,
        status = Status.UTREDES
    ),
    OMGJØRING(
        gruppe = StegGruppe.OMGJØRING,
        status = Status.IVERKSETTES
    ),
    TREKK_KLAGE(
        gruppe = StegGruppe.TREKK_KLAGE,
        status = Status.UTREDES
    ),
    OPPRETTHOLDELSE(
        gruppe = StegGruppe.OPPRETTHOLDELSE,
        status = Status.IVERKSETTES
    ),
    EFFEKTUER_AVVIST_PÅ_FORMKRAV(
        gruppe = StegGruppe.KLAGE_AVVIST_PÅ_FORMKRAV,
        status = Status.IVERKSETTES
    ),
    UDEFINERT(
        gruppe = StegGruppe.UDEFINERT,
        status = Status.UTREDES,
        tekniskSteg = true,
    ) // Forbeholdt deklarasjon for avklaringsbehov som
}