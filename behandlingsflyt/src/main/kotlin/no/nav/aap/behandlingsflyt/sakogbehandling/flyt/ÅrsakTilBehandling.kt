package no.nav.aap.behandlingsflyt.sakogbehandling.flyt

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling as EksponertÅrsak

enum class ÅrsakTilBehandling {
    MOTTATT_SØKNAD,
    MOTTATT_AKTIVITETSMELDING,
    MOTTATT_MELDEKORT,
    MOTTATT_LEGEERKLÆRING,
    MOTTATT_AVVIST_LEGEERKLÆRING,
    MOTTATT_DIALOGMELDING,
    MOTATT_KLAGE,
    SØKNAD_TRUKKET,
    KLAGE_TRUKKET,
    REVURDER_MEDLEMSKAP,
    REVURDER_SAMORDNING,
    REVURDER_LOVVALG,
    REVURDER_BEREGNING,         // Beregningstidspunkt
    REVURDER_YRKESSKADE,        // Yrkesskade
    REVURDER_MANUELL_INNTEKT,   // Manuell inntekt
    G_REGULERING,
    LOVVALG_OG_MEDLEMSKAP,      // Lovvalg og medlemskap
    FORUTGAENDE_MEDLEMSKAP,     // Forutgående medlemskap
    SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND, // Sykdom, arbeidsevne og behov for bistand
    BARNETILLEGG,               // Barnetillegg
    INSTITUSJONSOPPHOLD,        // Institusjonsopphold
    SAMORDNING_OG_AVREGNING,    // Samordning og avregning
    REFUSJONSKRAV,              // Refusjonskrav
    UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT, // Utenlandsopphold før søknadstidspunkt
    FASTSATT_PERIODE_PASSERT,
    FRITAK_MELDEPLIKT,
    VURDER_RETTIGHETSPERIODE,
    MOTTATT_KABAL_HENDELSE,
    OPPFØLGINGSOPPGAVE;

    companion object {
        /**
         * Alle med funksjonell verdi, G-regulering holdes utenfor
         */
        fun alle(): List<ÅrsakTilBehandling> {
            val alle = ÅrsakTilBehandling.entries.toMutableSet()

            alle.remove(G_REGULERING)

            return alle.toList()
        }

        /**
         * Alle med funksjonell verdi
         */
        fun alleInklusivGRegulering(): List<ÅrsakTilBehandling> {
            return ÅrsakTilBehandling.entries.toList()
        }
    }
}

fun EksponertÅrsak.tilÅrsakTilBehandling() =
    when (this) {
        EksponertÅrsak.SØKNAD -> ÅrsakTilBehandling.MOTTATT_SØKNAD
        EksponertÅrsak.AKTIVITETSMELDING -> ÅrsakTilBehandling.MOTTATT_AKTIVITETSMELDING
        EksponertÅrsak.MELDEKORT -> ÅrsakTilBehandling.MOTTATT_MELDEKORT
        EksponertÅrsak.LEGEERKLÆRING -> ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING
        EksponertÅrsak.AVVIST_LEGEERKLÆRING -> ÅrsakTilBehandling.MOTTATT_AVVIST_LEGEERKLÆRING
        EksponertÅrsak.DIALOGMELDING -> ÅrsakTilBehandling.MOTTATT_DIALOGMELDING
        EksponertÅrsak.G_REGULERING -> ÅrsakTilBehandling.G_REGULERING
        EksponertÅrsak.REVURDER_MEDLEMSKAP -> ÅrsakTilBehandling.REVURDER_MEDLEMSKAP
        EksponertÅrsak.REVURDER_YRKESSKADE -> ÅrsakTilBehandling.REVURDER_YRKESSKADE
        EksponertÅrsak.REVURDER_BEREGNING -> ÅrsakTilBehandling.REVURDER_BEREGNING
        EksponertÅrsak.REVURDER_LOVVALG -> ÅrsakTilBehandling.REVURDER_LOVVALG
        EksponertÅrsak.REVURDER_SAMORDNING -> ÅrsakTilBehandling.REVURDER_SAMORDNING
        EksponertÅrsak.KLAGE -> ÅrsakTilBehandling.MOTATT_KLAGE
        EksponertÅrsak.LOVVALG_OG_MEDLEMSKAP -> ÅrsakTilBehandling.LOVVALG_OG_MEDLEMSKAP
        EksponertÅrsak.FORUTGAENDE_MEDLEMSKAP -> ÅrsakTilBehandling.FORUTGAENDE_MEDLEMSKAP
        EksponertÅrsak.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND -> ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        EksponertÅrsak.BARNETILLEGG -> ÅrsakTilBehandling.BARNETILLEGG
        EksponertÅrsak.INSTITUSJONSOPPHOLD -> ÅrsakTilBehandling.INSTITUSJONSOPPHOLD
        EksponertÅrsak.SAMORDNING_OG_AVREGNING -> ÅrsakTilBehandling.SAMORDNING_OG_AVREGNING
        EksponertÅrsak.REFUSJONSKRAV -> ÅrsakTilBehandling.REFUSJONSKRAV
        EksponertÅrsak.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT -> ÅrsakTilBehandling.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT
        EksponertÅrsak.VURDER_RETTIGHETSPERIODE -> ÅrsakTilBehandling.VURDER_RETTIGHETSPERIODE
        EksponertÅrsak.SØKNAD_TRUKKET -> ÅrsakTilBehandling.SØKNAD_TRUKKET
        EksponertÅrsak.FRITAK_MELDEPLIKT -> ÅrsakTilBehandling.FRITAK_MELDEPLIKT
        EksponertÅrsak.KLAGE_TRUKKET -> ÅrsakTilBehandling.KLAGE_TRUKKET
        EksponertÅrsak.REVURDER_MANUELL_INNTEKT -> ÅrsakTilBehandling.REVURDER_MANUELL_INNTEKT
        EksponertÅrsak.MOTTATT_KABAL_HENDELSE -> ÅrsakTilBehandling.MOTTATT_KABAL_HENDELSE
        EksponertÅrsak.OPPFØLGINGSOPPGAVE -> ÅrsakTilBehandling.OPPFØLGINGSOPPGAVE
    }
