package no.nav.aap.behandlingsflyt.datadeling

import no.nav.aap.komponenter.type.Periode

data class SakStatus(
    val sakId: String,
    val status: DatadelingBehandlingStatus,
    val periode: Periode,
) {
    companion object {
        fun fromKelvin(saksnummer: String, datadelingBehandlingStatus: DatadelingBehandlingStatus, periode: Periode): SakStatus {
            return SakStatus(
                sakId = saksnummer,
                status = datadelingBehandlingStatus,
                periode = periode,
            )
        }
    }

    /**
     * Ment å deles til NKS via api-intern.
     */
    enum class DatadelingBehandlingStatus {
        SOKNAD_UNDER_BEHANDLING,
        REVURDERING_UNDER_BEHANDLING,
        FERDIGBEHANDLET,
        // Muligens også si noe om STANS/OPPHØR her
    }
}
