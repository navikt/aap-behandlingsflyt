package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

enum class TypeBrev {
    VEDTAK_AVSLAG,
    VEDTAK_INNVILGELSE,
    VEDTAK_ENDRING,
    VARSEL_OM_BESTILLING,
    FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
    KLAGE_AVVIST,
    KLAGE_OPPRETTHOLDELSE,
    KLAGE_TRUKKET;


    fun erVedtak(): Boolean {
        return setOf(
            VEDTAK_AVSLAG,
            VEDTAK_INNVILGELSE,
            VEDTAK_ENDRING,
            KLAGE_AVVIST,
            KLAGE_OPPRETTHOLDELSE,
            KLAGE_TRUKKET
        ).contains(this)
    }
}