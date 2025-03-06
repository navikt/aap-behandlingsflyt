package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

enum class TypeBrev {
    VEDTAK_AVSLAG,
    VEDTAK_INNVILGELSE,
    VEDTAK_ENDRING,
    VARSEL_OM_BESTILLING,
    FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT;


    fun erVedtak(): Boolean {
        return setOf(VEDTAK_AVSLAG, VEDTAK_INNVILGELSE).contains(this)
    }
}