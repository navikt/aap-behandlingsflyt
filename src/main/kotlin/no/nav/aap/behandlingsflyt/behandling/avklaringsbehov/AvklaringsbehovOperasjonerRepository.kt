package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

/**
 * Kun for bruk innad i Avklaringsbehovene
 */
interface AvklaringsbehovOperasjonerRepository {
    fun hentAvklaringsbehovene(behandlingId: BehandlingId): List<Avklaringsbehov>
    fun leggTilAvklaringsbehov(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType)

    @Deprecated("Vi ønsker å endre domeneobjektet, og la det lagre seg selv. Bruk `endre(avklaringsbehov)` i stedet.", replaceWith= ReplaceWith("endre"), DeprecationLevel.WARNING)
    fun endreAvklaringsbehov(avklaringsbehovId: Long, status: Status, begrunnelse: String, opprettetAv: String)
    fun endre(avklaringsbehov: Avklaringsbehov)
    fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean)
}