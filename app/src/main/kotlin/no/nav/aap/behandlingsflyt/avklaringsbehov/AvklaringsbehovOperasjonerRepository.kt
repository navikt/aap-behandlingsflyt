package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.flyt.StegType

/**
 * Kun for bruk innad i Avklaringsbehovene
 */
interface AvklaringsbehovOperasjonerRepository {
    fun hent(behandlingId: BehandlingId): List<Avklaringsbehov>
    fun opprett(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType)
    fun endre(avklaringsbehov: Avklaringsbehov)
    fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean)
}