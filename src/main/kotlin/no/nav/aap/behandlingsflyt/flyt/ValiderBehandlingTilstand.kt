package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.Status
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon

object ValiderBehandlingTilstand {
    fun validerTilstandBehandling(
        behandling: Behandling,
        avklaringsbehov: List<Definisjon> = listOf()
    ) {
        if (Status.AVSLUTTET == behandling.status()) {
            throw IllegalArgumentException("Forsøker manipulere på behandling som er avsluttet")
        }
        if (avklaringsbehov.any { !behandling.avklaringsbehov().map { a -> a.definisjon }.contains(it) }) {
            throw IllegalArgumentException("Forsøker løse avklaringsbehov $avklaringsbehov ikke knyttet til behandlingen, har ${behandling.avklaringsbehov()}")
        }
        if (avklaringsbehov.any {
                !behandling.type.flyt().erStegFørEllerLik(it.løsesISteg, behandling.aktivtSteg())
            }) {
            throw IllegalArgumentException("Forsøker løse avklaringsbehov $avklaringsbehov ikke knyttet til behandlingen")
        }
    }
}