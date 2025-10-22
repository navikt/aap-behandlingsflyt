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
    REVURDERING_AVBRUTT,
    KLAGE_TRUKKET,
    REVURDER_MEDLEMSKAP,
    REVURDER_SAMORDNING,
    REVURDER_LOVVALG,
    REVURDER_BEREGNING,         // Beregningstidspunkt
    REVURDER_YRKESSKADE,        // Yrkesskade
    REVURDER_STUDENT,
    REVURDER_MANUELL_INNTEKT,   // Manuell inntekt
    REVURDER_MELDEPLIKT_RIMELIG_GRUNN,
    REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER,  // Samordning andre folketrygdytelser
    REVURDER_SAMORDNING_UFØRE,                    // Samordning uføre
    REVURDER_SAMORDNING_ANDRE_STATLIGE_YTELSER,   // Samordning andre statlige ytelser
    REVURDER_SAMORDNING_ARBEIDSGIVER,             // Samordning arbeidsgiver
    REVURDER_SAMORDNING_TJENESTEPENSJON,          // Samordning tjenestepensjon
    G_REGULERING,
    LOVVALG_OG_MEDLEMSKAP,      // Lovvalg og medlemskap
    FORUTGAENDE_MEDLEMSKAP,     // Forutgående medlemskap
    OPPHOLDSKRAV,
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
    OPPFØLGINGSOPPGAVE,
    AKTIVITETSPLIKT_11_7,
    AKTIVITETSPLIKT_11_9,
    EFFEKTUER_AKTIVITETSPLIKT,
    EFFEKTUER_AKTIVITETSPLIKT_11_9,
    OVERGANG_UFORE,
    OVERGANG_ARBEID,
    DØDSFALL_BRUKER,
    DØDSFALL_BARN,
    ;

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
        EksponertÅrsak.REVURDER_SAMORDNING_ANDRE_FOKETRYGDYTELSER -> Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER
        EksponertÅrsak.REVURDER_SAMORDNING_UFØRE -> Vurderingsbehov.REVURDER_SAMORDNING_UFØRE
        EksponertÅrsak.REVURDER_SAMORDNING_ANDRE_STATLIGE_YTELSER -> Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_STATLIGE_YTELSER
        EksponertÅrsak.REVURDER_SAMORDNING_ARBEIDSGIVER -> Vurderingsbehov.REVURDER_SAMORDNING_ARBEIDSGIVER
        EksponertÅrsak.REVURDER_SAMORDNING_TJENESTEPENSJON -> Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON
        EksponertÅrsak.REVURDER_STUDENT -> Vurderingsbehov.REVURDER_STUDENT
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
        EksponertÅrsak.REVURDERING_AVBRUTT -> Vurderingsbehov.REVURDERING_AVBRUTT
        EksponertÅrsak.FRITAK_MELDEPLIKT -> Vurderingsbehov.FRITAK_MELDEPLIKT
        EksponertÅrsak.KLAGE_TRUKKET -> Vurderingsbehov.KLAGE_TRUKKET
        EksponertÅrsak.REVURDER_MANUELL_INNTEKT -> Vurderingsbehov.REVURDER_MANUELL_INNTEKT
        EksponertÅrsak.MOTTATT_KABAL_HENDELSE -> Vurderingsbehov.MOTTATT_KABAL_HENDELSE
        EksponertÅrsak.OPPFØLGINGSOPPGAVE -> Vurderingsbehov.OPPFØLGINGSOPPGAVE
        EksponertÅrsak.HELHETLIG_VURDERING -> Vurderingsbehov.HELHETLIG_VURDERING
        EksponertÅrsak.REVURDER_MELDEPLIKT_RIMELIG_GRUNN -> Vurderingsbehov.REVURDER_MELDEPLIKT_RIMELIG_GRUNN
        EksponertÅrsak.AKTIVITETSPLIKT_11_7 -> Vurderingsbehov.AKTIVITETSPLIKT_11_7
        EksponertÅrsak.AKTIVITETSPLIKT_11_9 -> Vurderingsbehov.AKTIVITETSPLIKT_11_9
        EksponertÅrsak.EFFEKTUER_AKTIVITETSPLIKT -> Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT
        EksponertÅrsak.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT_11_9
        EksponertÅrsak.OPPHOLDSKRAV -> Vurderingsbehov.OPPHOLDSKRAV
        EksponertÅrsak.OVERGANG_UFORE -> Vurderingsbehov.OVERGANG_UFORE
        EksponertÅrsak.OVERGANG_ARBEID -> Vurderingsbehov.OVERGANG_ARBEID
        EksponertÅrsak.DØDSFALL_BRUKER -> Vurderingsbehov.DØDSFALL_BRUKER
        EksponertÅrsak.DØDSFALL_BARN -> Vurderingsbehov.DØDSFALL_BARN
    }
