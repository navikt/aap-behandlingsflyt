package no.nav.aap.behandlingsflyt.datadeling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.type.Periode

data class SakStatusDTO(
    val ident: String,
    val status: SakStatus
)

data class SakStatus(
    val sakId: String,
    @Deprecated("Bruk status. Denne må skrives bort fra api-intern.")
    val statusKode: VedtakStatus,
    val status: SakOgBehandlingstatus,
    val periode: Periode,
) {
    companion object {
        fun fromKelvin(saksnummer: String, status: Status, sakOgBehandlingstatus: SakOgBehandlingstatus, periode: Periode): SakStatus {
            return SakStatus(
                sakId = saksnummer,
                statusKode = VedtakStatus.valueOf(status.toString()),
                status = sakOgBehandlingstatus,
                periode = periode,
            )
        }
    }

    enum class VedtakStatus {
        OPPRETTET,
        UTREDES,
        LØPENDE,
        AVSLUTTET
    }

    /**
     * Ment å deles til NKS via api-intern.
     */
    enum class SakOgBehandlingstatus {
        SOKNAD_UNDER_BEHANDLING,
        REVURDERING_UNDER_BEHANDLING,
        FERDIGBEHANDLET,
        // Muligens også si noe om STANS/OPPHØR her
    }
}
