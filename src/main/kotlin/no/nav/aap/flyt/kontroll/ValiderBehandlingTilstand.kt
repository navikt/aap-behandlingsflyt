package no.nav.aap.flyt.kontroll

import no.nav.aap.domene.behandling.Behandling
import no.nav.aap.domene.behandling.Status
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon

object ValiderBehandlingTilstand {
    fun validerTilstandBehandling(behandling: Behandling,
                                  avklaringsbehov: List<Definisjon> = listOf()) {
        if (setOf(Status.AVSLUTTET, Status.IVERKSETTES).contains(behandling.status())) {
            throw IllegalArgumentException("Forsøker manipulere på behandling som er avsluttet")
        }
        if (avklaringsbehov.any { !behandling.avklaringsbehov().map { a -> a.definisjon }.contains(it) }) {
            throw IllegalArgumentException("Forsøker løse aksjonspunkt ikke knyttet til behandlingen")
        }
        if (avklaringsbehov.any {
                !behandling.type.flyt().erStegFørEllerLik(it.løsesISteg, behandling.aktivtSteg().tilstand.steg())
            }) {
            throw IllegalArgumentException("Forsøker løse aksjonspunkt ikke knyttet til behandlingen")
        }
    }
}