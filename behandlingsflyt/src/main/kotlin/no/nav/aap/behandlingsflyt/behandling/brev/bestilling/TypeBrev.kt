package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

enum class TypeBrev {
    VEDTAK_AVSLAG,
    VEDTAK_INNVILGELSE,
    VEDTAK_UTVID_VEDTAKSLENGDE,
    VEDTAK_11_17,
    VEDTAK_11_18,
    VEDTAK_ENDRING,
    BARNETILLEGG_SATS_REGULERING,
    VEDTAK_11_7,
    VEDTAK_11_9,
    VEDTAK_11_23_SJETTE_LEDD,
    VARSEL_OM_BESTILLING,
    FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
    FORHÅNDSVARSEL_KLAGE_FORMKRAV,
    KLAGE_AVVIST,
    KLAGE_OPPRETTHOLDELSE,
    KLAGE_TRUKKET,
    KLAGE_MOTTATT,
    FORVALTNINGSMELDING;

    fun erVedtak(): Boolean {
        return setOf(
            VEDTAK_AVSLAG,
            VEDTAK_INNVILGELSE,
            VEDTAK_UTVID_VEDTAKSLENGDE,
            VEDTAK_11_17,
            VEDTAK_11_18,
            VEDTAK_11_7,
            VEDTAK_11_9,
            VEDTAK_11_23_SJETTE_LEDD,
            VEDTAK_ENDRING,
            BARNETILLEGG_SATS_REGULERING,
            KLAGE_AVVIST,
            KLAGE_OPPRETTHOLDELSE,
            KLAGE_TRUKKET
        ).contains(this)
    }
}