package no.nav.aap.verdityper.flyt

enum class StegGruppe(val måVises: Boolean) {
    START_BEHANDLING(false),
    ALDER(true),
    LOVVALG(true),
    MEDLEMSKAP(true),
    BARNETILLEGG(true),
    STUDENT(false),
    SYKDOM(true),
    GRUNNLAG(true),
    ET_ANNET_STED(false),
    UNDERVEIS(true),
    TILKJENT_YTELSE(true),
    SIMULERING(true),
    VEDTAK(true),
    FATTE_VEDTAK(true),
    KVALITETSSIKRING(true),
    IVERKSETT_VEDTAK(true),
    UDEFINERT(true)
}
