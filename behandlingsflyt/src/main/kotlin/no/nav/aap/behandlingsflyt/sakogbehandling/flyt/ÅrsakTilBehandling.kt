package no.nav.aap.behandlingsflyt.sakogbehandling.flyt

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling as EksponertÅrsak

enum class ÅrsakTilBehandling {
    MOTTATT_SØKNAD,
    MOTTATT_AKTIVITETSMELDING,
    MOTTATT_MELDEKORT,
    MOTTATT_LEGEERKLÆRING,
    MOTTATT_AVVIST_LEGEERKLÆRING,
    MOTTATT_DIALOGMELDING,
    REVURDER_MEDLEMSKAP,
    REVURDER_BEREGNING,
    REVURDER_YRKESSKADE,
    G_REGULERING;

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
    }