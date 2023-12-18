package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

/**
 * Kun for bruk innad i Avklaringsbehovene
 */
interface AvklaringsbehovOperasjonerRepository {
    fun leggTilAvklaringsbehov(behandlingId: BehandlingId, definisjoner: List<Definisjon>, funnetISteg: StegType)
    fun leggTilAvklaringsbehov(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType)
    fun løs(behandlingId: BehandlingId, definisjon: Definisjon, begrunnelse: String, kreverToTrinn: Boolean?)
    fun hentBehovene(behandlingId: BehandlingId): List<Avklaringsbehov>
    fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean)
    fun endreAvklaringsbehov(avklaringsbehovId: Long, status: Status, begrunnelse: String, opprettetAv: String)
}