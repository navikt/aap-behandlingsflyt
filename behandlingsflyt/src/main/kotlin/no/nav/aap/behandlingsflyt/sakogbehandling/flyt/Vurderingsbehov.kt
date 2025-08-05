package no.nav.aap.behandlingsflyt.sakogbehandling.flyt

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov as EksponertÅrsak

enum class Vurderingsbehov {
    MOTTATT_SØKNAD,
    HELHETLIG_VURDERING,
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
        fun alle(): List<Vurderingsbehov> {
            val alle = Vurderingsbehov.entries.toMutableSet()

            alle.remove(G_REGULERING)

            return alle.toList()
        }

        /**
         * Alle med funksjonell verdi
         */
        fun alleInklusivGRegulering(): List<Vurderingsbehov> {
            return Vurderingsbehov.entries.toList()
        }
    }
}

fun EksponertÅrsak.tilVurderingsbehov() =
    when (this) {
        EksponertÅrsak.SØKNAD -> Vurderingsbehov.MOTTATT_SØKNAD
        EksponertÅrsak.AKTIVITETSMELDING -> Vurderingsbehov.MOTTATT_AKTIVITETSMELDING
        EksponertÅrsak.MELDEKORT -> Vurderingsbehov.MOTTATT_MELDEKORT
        EksponertÅrsak.LEGEERKLÆRING -> Vurderingsbehov.MOTTATT_LEGEERKLÆRING
        EksponertÅrsak.AVVIST_LEGEERKLÆRING -> Vurderingsbehov.MOTTATT_AVVIST_LEGEERKLÆRING
        EksponertÅrsak.DIALOGMELDING -> Vurderingsbehov.MOTTATT_DIALOGMELDING
        EksponertÅrsak.G_REGULERING -> Vurderingsbehov.G_REGULERING
        EksponertÅrsak.REVURDER_MEDLEMSKAP -> Vurderingsbehov.REVURDER_MEDLEMSKAP
        EksponertÅrsak.REVURDER_YRKESSKADE -> Vurderingsbehov.REVURDER_YRKESSKADE
        EksponertÅrsak.REVURDER_BEREGNING -> Vurderingsbehov.REVURDER_BEREGNING
        EksponertÅrsak.REVURDER_LOVVALG -> Vurderingsbehov.REVURDER_LOVVALG
        EksponertÅrsak.REVURDER_SAMORDNING -> Vurderingsbehov.REVURDER_SAMORDNING
        EksponertÅrsak.KLAGE -> Vurderingsbehov.MOTATT_KLAGE
        EksponertÅrsak.LOVVALG_OG_MEDLEMSKAP -> Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP
        EksponertÅrsak.FORUTGAENDE_MEDLEMSKAP -> Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP
        EksponertÅrsak.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND -> Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        EksponertÅrsak.BARNETILLEGG -> Vurderingsbehov.BARNETILLEGG
        EksponertÅrsak.INSTITUSJONSOPPHOLD -> Vurderingsbehov.INSTITUSJONSOPPHOLD
        EksponertÅrsak.SAMORDNING_OG_AVREGNING -> Vurderingsbehov.SAMORDNING_OG_AVREGNING
        EksponertÅrsak.REFUSJONSKRAV -> Vurderingsbehov.REFUSJONSKRAV
        EksponertÅrsak.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT -> Vurderingsbehov.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT
        EksponertÅrsak.VURDER_RETTIGHETSPERIODE -> Vurderingsbehov.VURDER_RETTIGHETSPERIODE
        EksponertÅrsak.SØKNAD_TRUKKET -> Vurderingsbehov.SØKNAD_TRUKKET
        EksponertÅrsak.FRITAK_MELDEPLIKT -> Vurderingsbehov.FRITAK_MELDEPLIKT
        EksponertÅrsak.KLAGE_TRUKKET -> Vurderingsbehov.KLAGE_TRUKKET
        EksponertÅrsak.REVURDER_MANUELL_INNTEKT -> Vurderingsbehov.REVURDER_MANUELL_INNTEKT
        EksponertÅrsak.MOTTATT_KABAL_HENDELSE -> Vurderingsbehov.MOTTATT_KABAL_HENDELSE
        EksponertÅrsak.OPPFØLGINGSOPPGAVE -> Vurderingsbehov.OPPFØLGINGSOPPGAVE
        EksponertÅrsak.HELHETLIG_VURDERING -> Vurderingsbehov.HELHETLIG_VURDERING
    }
