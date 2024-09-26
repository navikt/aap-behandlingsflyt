package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling

internal object ValiderBehandlingTilstand {

    fun validerTilstandBehandling(
        behandling: Behandling,
        avklaringsbehov: Definisjon?,
        eksisterendeAvklaringsbehov: List<Avklaringsbehov>
    ) {
        validerStatus(behandling.status())
        if (avklaringsbehov != null) {
            if (!eksisterendeAvklaringsbehov.map { a -> a.definisjon }
                    .contains(avklaringsbehov) && !avklaringsbehov.erFrivillig()) {
                throw IllegalArgumentException("Forsøker å løse avklaringsbehov $avklaringsbehov ikke knyttet til behandlingen, har $eksisterendeAvklaringsbehov")
            }
            val flyt = utledType(behandling.typeBehandling()).flyt()
            if (!flyt.erStegFørEllerLik(avklaringsbehov.løsesISteg, behandling.aktivtSteg())) {
                throw IllegalArgumentException(
                    "Forsøker å løse avklaringsbehov $avklaringsbehov som er definert i et steg etter nåværende steg[${behandling.aktivtSteg()}] ${
                        behandling.typeBehandling().identifikator()
                    }"
                )
            }
        }
    }

    /**
     * Valider om behandlingen er i en tilstand hvor det er OK å skrive til den
     */
    fun validerTilstandBehandling(behandling: Behandling, versjon: Long) {
        validerStatus(behandling.status())
        if (behandling.versjon != versjon) {
            throw OutdatedBehandlingException("Behandlingen har blitt oppdatert. Versjonsnummer[$versjon] ulikt fra siste[${behandling.versjon}]")
        }
    }

    private fun validerStatus(behandlingStatus: Status) {
        if (Status.AVSLUTTET == behandlingStatus) {
            throw IllegalArgumentException("Forsøker manipulere på behandling som er avsluttet")
        }
    }
}