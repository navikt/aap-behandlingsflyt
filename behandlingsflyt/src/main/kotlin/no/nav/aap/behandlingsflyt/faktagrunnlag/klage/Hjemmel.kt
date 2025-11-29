package no.nav.aap.behandlingsflyt.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov

// TODO: Hjemler her som er markert som TODO er ting som kan klages på
// men det er ikke implementert noen revurdering på de, så det vil feile
enum class Hjemmel(val hjemmel: String) {
    FOLKETRYGDLOVEN_KAPITTEL_2("Kapittel 2"),
    FOLKETRYGDLOVEN_11_2("§ 11-2"),

    FOLKETRYGDLOVEN_11_3("§ 11-3"),
    FOLKETRYGDLOVEN_11_4("§ 11-4"), // TODO: Hva klages på her?
    FOLKETRYGDLOVEN_11_5("§ 11-5"),
    FOLKETRYGDLOVEN_11_6("§ 11-6"),

    FOLKETRYGDLOVEN_11_7("§ 11-7"), // TODO: Iverksett sanksjon / Tors hammer - Mangler steg
    FOLKETRYGDLOVEN_11_8("§ 11-8"), // TODO: Iverksett sanksjon / Tors hammer - Mangler steg
    FOLKETRYGDLOVEN_11_9("§ 11-9"), // TODO: Iverksett sanksjon / Tors hammer - Mangler steg
    FOLKETRYGDLOVEN_11_10_FRITAK("§ 11-10 Fritak meldeplikt"),

    FOLKETRYGDLOVEN_11_10_MELDEPLIKT("§ 11-10 Meldeplikt"), // TODO: Underveis - Mangler mulighet til å korrigere meldedato
    FOLKETRYGDLOVEN_11_12("§ 11-12"), // TODO: Hva klages på  her?
    FOLKETRYGDLOVEN_11_13("§ 11-13"),

    FOLKETRYGDLOVEN_11_14("§ 11-14"), // TODO: Mangler revurdering student - må evt. sende ny søknad?
    FOLKETRYGDLOVEN_11_15("§ 11-15"), // TODO: Mangler steg
    FOLKETRYGDLOVEN_11_17("§ 11-17"),
    FOLKETRYGDLOVEN_11_18("§ 11-18"),
    FOLKETRYGDLOVEN_11_19("§ 11-19"),
    FOLKETRYGDLOVEN_11_20("§ 11-20"),
    FOLKETRYGDLOVEN_11_22("§ 11-22"),
    FOLKETRYGDLOVEN_11_23_UUTNYTTET_ARB_EVNE("§ 11-23 2. ledd"),

    FOLKETRYGDLOVEN_11_23_OVERGNG_ARB("§ 11-23 6. ledd"), // TODO: Mangler steg
    FOLKETRYGDLOVEN_11_24("§ 11-24"),
    FOLKETRYGDLOVEN_11_25("§ 11-25"),
    FOLKETRYGDLOVEN_11_26("§ 11-26"),
    FOLKETRYGDLOVEN_11_27("§ 11-27"),
    FOLKETRYGDLOVEN_11_28("§ 11-28"),
    FOLKETRYGDLOVEN_11_29("§ 11-29"), // TODO: Hva kan klages på her?

    // FOLKETRYGDLOVEN_11_30("§ 11-30"), // Ikke relevant
    FOLKETRYGDLOVEN_11_31("§ 11-31"), // TODO: Hva kan klages på her?

    FOLKETRYGDLOVEN_21_3("§ 21-3"), // Ikke aktuelt som hjemmel for omgjøring
    FOLKETRYGDLOVEN_21_7("§ 21-7"), // Ikke aktuelt som hjemmel for omgjøring
    FOLKETRYGDLOVEN_22_13("§ 22-13"), // TODO: Hva kan klages på her?
    FOLKETRYGDLOVEN_22_15("§ 22-15"), // TODO: Må videre til Team Tilbake?
    FOLKETRYGDLOVEN_22_17("§ 22-17"); // TODO: Hva kan klages på her?
    // AVREGNING("Avregning"); // Kan ikke klage på avregning

    companion object {
        fun fraHjemmel(hjemmel: String): Hjemmel? {
            return entries.firstOrNull { it.hjemmel == hjemmel }
        }
    }

    fun tilVurderingsbehov(): List<Vurderingsbehov> {
        return when (this) {
            FOLKETRYGDLOVEN_11_2 -> listOf(Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP)
            FOLKETRYGDLOVEN_11_3 -> listOf(Vurderingsbehov.OPPHOLDSKRAV)

            FOLKETRYGDLOVEN_11_5,
            FOLKETRYGDLOVEN_11_6,
            FOLKETRYGDLOVEN_11_10_FRITAK,
            FOLKETRYGDLOVEN_11_13,
            FOLKETRYGDLOVEN_11_17,
            FOLKETRYGDLOVEN_11_18,
            FOLKETRYGDLOVEN_11_23_UUTNYTTET_ARB_EVNE
                -> listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)

            FOLKETRYGDLOVEN_11_10_MELDEPLIKT -> listOf(Vurderingsbehov.REVURDER_MELDEPLIKT_RIMELIG_GRUNN)

            FOLKETRYGDLOVEN_11_19 -> listOf(Vurderingsbehov.REVURDER_BEREGNING)

            FOLKETRYGDLOVEN_11_20 -> listOf(Vurderingsbehov.BARNETILLEGG)

            FOLKETRYGDLOVEN_11_22 -> listOf(Vurderingsbehov.REVURDER_YRKESSKADE)

            FOLKETRYGDLOVEN_11_25,
            FOLKETRYGDLOVEN_11_26 -> listOf(Vurderingsbehov.INSTITUSJONSOPPHOLD)

            FOLKETRYGDLOVEN_11_24,
            FOLKETRYGDLOVEN_11_27,
            FOLKETRYGDLOVEN_11_28 -> listOf(Vurderingsbehov.SAMORDNING_OG_AVREGNING)

            FOLKETRYGDLOVEN_KAPITTEL_2 -> listOf(Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP)

            FOLKETRYGDLOVEN_22_13 -> listOf(Vurderingsbehov.HELHETLIG_VURDERING, Vurderingsbehov.VURDER_RETTIGHETSPERIODE)

            FOLKETRYGDLOVEN_11_4,
            FOLKETRYGDLOVEN_11_7,
            FOLKETRYGDLOVEN_11_8,
            FOLKETRYGDLOVEN_11_9,
            FOLKETRYGDLOVEN_11_12,
            FOLKETRYGDLOVEN_11_14,
            FOLKETRYGDLOVEN_11_15,
            FOLKETRYGDLOVEN_11_23_OVERGNG_ARB,
            FOLKETRYGDLOVEN_11_29,
            FOLKETRYGDLOVEN_11_31,
            FOLKETRYGDLOVEN_21_3,
            FOLKETRYGDLOVEN_21_7,
            FOLKETRYGDLOVEN_22_15,
            FOLKETRYGDLOVEN_22_17 -> throw IllegalStateException("Ingen ÅrsakTilBehandling-mapping er implementert for klage på hjemmel $name ($hjemmel).")
        }
    }
}
