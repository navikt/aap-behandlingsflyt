package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.Status
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon

object ValiderBehandlingTilstand {

    fun validerTilstandBehandling(
        behandling: Behandling,
        avklaringsbehov: List<Definisjon> = listOf(),
        eksisterenedeAvklaringsbehov: List<Avklaringsbehov>,
        versjon: Long
    ) {
        validerTilstandBehandling(behandling, avklaringsbehov, eksisterenedeAvklaringsbehov)

        //if (behandling.versjon != versjon) {
        //    throw OutdatedBehandlingException("Behandlingen har blitt oppdatert. Versjonsnummer ulikt fra siste")
        //}
    }

    fun validerTilstandBehandling(
        behandling: Behandling,
        avklaringsbehov: List<Definisjon> = listOf(),
        eksisterenedeAvklaringsbehov: List<Avklaringsbehov>
    ) {
        if (Status.AVSLUTTET == behandling.status()) {
            throw IllegalArgumentException("Forsøker manipulere på behandling som er avsluttet")
        }
        if (avklaringsbehov.any { !eksisterenedeAvklaringsbehov.map { a -> a.definisjon }.contains(it) }) {
            throw IllegalArgumentException("Forsøker løse avklaringsbehov $avklaringsbehov ikke knyttet til behandlingen, har $eksisterenedeAvklaringsbehov")
        }
        if (avklaringsbehov.any {
                !behandling.type.flyt().erStegFørEllerLik(it.løsesISteg, behandling.aktivtSteg())
            }) {
            throw IllegalArgumentException("Forsøker løse avklaringsbehov $avklaringsbehov ikke knyttet til behandlingen")
        }
    }
}