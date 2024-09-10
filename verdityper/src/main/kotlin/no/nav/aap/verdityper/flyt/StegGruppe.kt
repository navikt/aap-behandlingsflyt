package no.nav.aap.verdityper.flyt

enum class StegGruppe(val skalVises: Boolean, val obligatoriskVisning: Boolean) {
    START_BEHANDLING(false, true),
    ALDER(true, true),
    LOVVALG(true, true),
    MEDLEMSKAP(true, true),
    BARNETILLEGG(true, true),
    STUDENT(true, false),
    SYKDOM(true, true),
    GRUNNLAG(true, true),
    ET_ANNET_STED(true, false),
    UNDERVEIS(true, true),
    TILKJENT_YTELSE(true, true),
    SIMULERING(true, true),
    VEDTAK(true, true),
    FATTE_VEDTAK(true, true),
    KVALITETSSIKRING(false, true),
    IVERKSETT_VEDTAK(false, true),
    BREV(false, true),
    UDEFINERT(false, true)
}