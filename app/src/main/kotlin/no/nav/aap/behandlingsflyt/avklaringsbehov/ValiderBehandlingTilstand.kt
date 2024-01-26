package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.verdityper.sakogbehandling.Status

internal object ValiderBehandlingTilstand {

    fun validerTilstandBehandling(
        behandling: Behandling,
        avklaringsbehov: Definisjon?,
        eksisterenedeAvklaringsbehov: List<Avklaringsbehov>,
        versjon: Long? = null
    ) {
        validerTilstandBehandling(behandling, avklaringsbehov, eksisterenedeAvklaringsbehov)

        //if (behandling.versjon != versjon) {
        //    throw OutdatedBehandlingException("Behandlingen har blitt oppdatert. Versjonsnummer ulikt fra siste")
        //}
    }

    fun validerTilstandBehandling(
        behandling: Behandling,
        avklaringsbehov: Definisjon? = null,
        eksisterenedeAvklaringsbehov: List<Avklaringsbehov>
    ) {
        if (Status.AVSLUTTET == behandling.status()) {
            throw IllegalArgumentException("Forsøker manipulere på behandling som er avsluttet")
        }
        if (avklaringsbehov != null) {
            if (!eksisterenedeAvklaringsbehov.map { a -> a.definisjon }
                    .contains(avklaringsbehov) && !avklaringsbehov.erFrivillig()) {
                throw IllegalArgumentException("Forsøker løse avklaringsbehov $avklaringsbehov ikke knyttet til behandlingen, har $eksisterenedeAvklaringsbehov")
            }
            val flyt = utledType(behandling.typeBehandling()).flyt()
            if (!flyt.erStegFørEllerLik(avklaringsbehov.løsesISteg, behandling.aktivtSteg())) {
                throw IllegalArgumentException("Forsøker løse avklaringsbehov $avklaringsbehov knyttet til et steg som ikke finnes i behandlingen av type ${behandling.typeBehandling().identifikator()}")
            }
        }
    }
}