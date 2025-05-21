package no.nav.aap.behandlingsflyt.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling

// TODO: Fyll på, evt. se om vi skal bruke Vilkårtype
enum class Hjemmel(val hjemmel: String) {
    FOLKETRYGDLOVEN_11_2("§ 11-2"),
    FOLKETRYGDLOVEN_11_5("§ 11-5"),
    FOLKETRYGDLOVEN_11_6("§ 11-6"),
    FOLKETRYGDLOVEN_11_13("§ 11-13"),
    FOLKETRYGDLOVEN_11_17("§ 11-17"),
    FOLKETRYGDLOVEN_11_18("§ 11-18"),
    FOLKETRYGDLOVEN_11_20("§ 11-20"),
    FOLKETRYGDLOVEN_11_22("§ 11-22"),
    FOLKETRYGDLOVEN_11_24("§ 11-24");

    companion object {
        fun fraHjemmel(hjemmel: String): Hjemmel? {
            return entries.firstOrNull { it.hjemmel == hjemmel }
        }
    }

    fun tilÅrsak(): ÅrsakTilBehandling {
        return when (this) {
            FOLKETRYGDLOVEN_11_2 -> ÅrsakTilBehandling.FORUTGAENDE_MEDLEMSKAP

            FOLKETRYGDLOVEN_11_5,
            FOLKETRYGDLOVEN_11_6,
            FOLKETRYGDLOVEN_11_13,
            FOLKETRYGDLOVEN_11_17,
            FOLKETRYGDLOVEN_11_18
                -> ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND

            FOLKETRYGDLOVEN_11_20 -> ÅrsakTilBehandling.BARNETILLEGG
            FOLKETRYGDLOVEN_11_22 -> ÅrsakTilBehandling.REVURDER_YRKESSKADE
            FOLKETRYGDLOVEN_11_24 -> ÅrsakTilBehandling.SAMORDNING_OG_AVREGNING
        }
    }
}