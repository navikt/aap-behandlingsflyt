package no.nav.aap.behandlingsflyt.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov

// TODO: Hjemler her som er markert som TODO er ting som kan klages på
// men det er ikke implementert noen revurdering på de, så det vil feile
enum class Hjemmel(val hjemmel: String) {
    FOLKETRYGDLOVEN_21_12("§ 21-12"),
    FVL_31("Fvl. § 31"),

    EOES_883_2004("EØS-forordningen (lovvalg / medlemskap)"),

    FOLKETRYGDLOVEN_KAPITTEL_2("Kapittel 2"),
    FOLKETRYGDLOVEN_11_2("§ 11-2"),

    FOLKETRYGDLOVEN_11_3("§ 11-3"),
    FOLKETRYGDLOVEN_11_4("§ 11-4"),
    FOLKETRYGDLOVEN_11_4_INNTEKTSBORTFALL("§ 11-4 2. ledd"),
    FOLKETRYGDLOVEN_11_5("§ 11-5"),
    FOLKETRYGDLOVEN_11_6("§ 11-6"),

    FOLKETRYGDLOVEN_11_7("§ 11-7"),
    FOLKETRYGDLOVEN_11_8("§ 11-8"),
    FOLKETRYGDLOVEN_11_9("§ 11-9"),
    FOLKETRYGDLOVEN_11_10_FRITAK("§ 11-10 Fritak meldeplikt"),

    FOLKETRYGDLOVEN_11_10_MELDEPLIKT("§ 11-10 Meldeplikt"), // TODO: Underveis - Mangler mulighet til å korrigere meldedato
    FOLKETRYGDLOVEN_11_12("§ 11-12"),
    FOLKETRYGDLOVEN_11_13("§ 11-13"),

    FOLKETRYGDLOVEN_11_14("§ 11-14"),
    FOLKETRYGDLOVEN_11_15("§ 11-15"),
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
    FOLKETRYGDLOVEN_11_29_SYKESTIPEND("§ 11-29"), // TODO: Hva kan klages på her?

    // FOLKETRYGDLOVEN_11_30("§ 11-30"), // Ikke relevant
    FOLKETRYGDLOVEN_11_31("§ 11-31"), // TODO: Hva kan klages på her?

    FOLKETRYGDLOVEN_21_3("§ 21-3"), // Ikke aktuelt som hjemmel for omgjøring
    FOLKETRYGDLOVEN_21_7("§ 21-7"), // Ikke aktuelt som hjemmel for omgjøring
    FOLKETRYGDLOVEN_22_13("§ 22-13"), // TODO: Hva kan klages på her?
    FOLKETRYGDLOVEN_22_15("§ 22-15"), // TODO: Må videre til Team Tilbake?
    FOLKETRYGDLOVEN_22_17("§ 22-17"), // TODO: Hva kan klages på her?
    ANDRE_TRYGDEAVTALER("Andre bilaterale trygdeavtaler");

    companion object {
        fun fraHjemmel(hjemmel: String): Hjemmel? {
            return entries.firstOrNull { it.hjemmel == hjemmel }
        }
    }

    fun tilVurderingsbehov(): List<Vurderingsbehov> {
        return when (this) {
            // Klage
            FOLKETRYGDLOVEN_21_12, FVL_31 -> throw IllegalStateException("Klage på avvisningsvedtak skal er kun gyldig for opprettholdelse")

            // Aktivitetspliktbehandling
            FOLKETRYGDLOVEN_11_7 -> listOf(Vurderingsbehov.AKTIVITETSPLIKT_11_7)
            FOLKETRYGDLOVEN_11_9 -> listOf(Vurderingsbehov.AKTIVITETSPLIKT_11_9)

            // Førstegangsbehandling / Revurdering
            FOLKETRYGDLOVEN_11_2 -> listOf(Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP)
            FOLKETRYGDLOVEN_11_3 -> listOf(Vurderingsbehov.OPPHOLDSKRAV)

            FOLKETRYGDLOVEN_11_4_INNTEKTSBORTFALL -> listOf(Vurderingsbehov.REVURDER_INNTEKTSBORTFALL)

            FOLKETRYGDLOVEN_11_5,
            FOLKETRYGDLOVEN_11_6,
            FOLKETRYGDLOVEN_11_10_FRITAK,
            FOLKETRYGDLOVEN_11_13, // TODO: Få vurderingsbehov for 11-13 til å funke dersom 11-5 og 11-6 er i "feil tilstand" for at 11-13 er relevant (se 11-18 og 11-17)
            FOLKETRYGDLOVEN_11_23_UUTNYTTET_ARB_EVNE
                -> listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)

            FOLKETRYGDLOVEN_11_10_MELDEPLIKT -> listOf(Vurderingsbehov.REVURDER_MELDEPLIKT_RIMELIG_GRUNN)

            FOLKETRYGDLOVEN_11_17 -> listOf(Vurderingsbehov.OVERGANG_ARBEID)
            FOLKETRYGDLOVEN_11_18 -> listOf(Vurderingsbehov.OVERGANG_UFORE)
            FOLKETRYGDLOVEN_11_19 -> listOf(Vurderingsbehov.REVURDER_BEREGNING)

            FOLKETRYGDLOVEN_11_20 -> listOf(Vurderingsbehov.BARNETILLEGG)

            FOLKETRYGDLOVEN_11_22 -> listOf(Vurderingsbehov.REVURDER_YRKESSKADE)

            FOLKETRYGDLOVEN_11_25,
            FOLKETRYGDLOVEN_11_26 -> listOf(Vurderingsbehov.INSTITUSJONSOPPHOLD)

            FOLKETRYGDLOVEN_11_24,
            FOLKETRYGDLOVEN_11_27,
            FOLKETRYGDLOVEN_11_28 -> listOf(Vurderingsbehov.SAMORDNING_OG_AVREGNING)

            ANDRE_TRYGDEAVTALER,
            EOES_883_2004,
            FOLKETRYGDLOVEN_KAPITTEL_2 -> listOf(Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP)

            FOLKETRYGDLOVEN_22_13 -> listOf(
                Vurderingsbehov.HELHETLIG_VURDERING,
                Vurderingsbehov.VURDER_RETTIGHETSPERIODE
            )

            FOLKETRYGDLOVEN_11_14 -> listOf(Vurderingsbehov.REVURDER_STUDENT)
            FOLKETRYGDLOVEN_11_15 -> listOf(Vurderingsbehov.ETABLERING_EGEN_VIRKSOMHET)
            FOLKETRYGDLOVEN_11_23_OVERGNG_ARB -> listOf(Vurderingsbehov.VURDER_ARBEIDSOPPTRAPPING)
            FOLKETRYGDLOVEN_11_29_SYKESTIPEND -> listOf(Vurderingsbehov.REVURDER_SYKESTIPEND)

            FOLKETRYGDLOVEN_11_4, // TODO: Hva klages på her?
            FOLKETRYGDLOVEN_11_8, // TODO: Ikke implementert
            FOLKETRYGDLOVEN_11_12, // TODO: Ikke implementert
            FOLKETRYGDLOVEN_11_31, // TODO: Hva skal trigges her? Revurder krav?
            FOLKETRYGDLOVEN_21_3, // Ikke aktuell?
            FOLKETRYGDLOVEN_21_7, // Ikke aktuell?
            FOLKETRYGDLOVEN_22_15, // TODO: Opprett tilbakekreving
            FOLKETRYGDLOVEN_22_17 // Ikke aktuell?
                -> throw IllegalStateException("Ingen ÅrsakTilBehandling-mapping er implementert for klage på hjemmel $name ($hjemmel).")
        }
    }
}
