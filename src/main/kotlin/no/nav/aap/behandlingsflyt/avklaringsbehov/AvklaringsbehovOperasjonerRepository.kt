package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

/**
 * Kun for bruk innad i Avklaringsbehovene
 */
interface AvklaringsbehovOperasjonerRepository {
    fun hent(behandlingId: BehandlingId): List<Avklaringsbehov>
    fun opprett(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType)
    fun endre(avklaringsbehov: Avklaringsbehov)
    fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean)
}