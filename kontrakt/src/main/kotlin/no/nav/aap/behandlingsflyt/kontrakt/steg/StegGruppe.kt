package no.nav.aap.behandlingsflyt.kontrakt.steg

public enum class StegGruppe(public val skalVises: Boolean, public val obligatoriskVisning: Boolean) {

    // Førstegangsbehandling/revurdering
    START_BEHANDLING(false, true),
    SEND_FORVALTNINGSMELDING(false, true),
    RETTIGHETSPERIODE(true, false),
    SØKNAD(true, false),
    KANSELLER_REVURDERING(true, false),
    ALDER(true, true),
    LOVVALG(true, true),
    MEDLEMSKAP(true, true),
    BARNETILLEGG(true, false),
    STUDENT(true, false),
    SYKDOM(true, true),
    GRUNNLAG(true, true),
    ET_ANNET_STED(true, false),
    SAMORDNING(skalVises = true, obligatoriskVisning = true),
    UNDERVEIS(true, true),
    TILKJENT_YTELSE(true, true),
    SIMULERING(true, true),
    VEDTAK(true, true),
    FATTE_VEDTAK(true, true),
    KVALITETSSIKRING(false, true),
    IVERKSETT_VEDTAK(false, true),
    BREV(true, true),
    UDEFINERT(false, true),

    // Klage
    FORMKRAV(true, true),
    KLAGEBEHANDLING_KONTOR(true, false),
    KLAGEBEHANDLING_NAY(true, false),
    OMGJØRING(true, false),
    TREKK_KLAGE(true, false),
    OPPRETTHOLDELSE(true, false),
    SVAR_FRA_ANDREINSTANS(true, true),
    IVERKSETT_KONSEKVENS(true, true),

    // Oppfølgingsbehandling
    START_OPPFØLGINGSBEHANDLING(false, false),
    AVKLAR_OPPPFØLGING(true, true),
    
    // Aktivitetsplikt
    AKTIVITETSPLIKT_11_7(true, true)
}