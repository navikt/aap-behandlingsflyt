package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate

/**
 * Kun for bruk innad i Avklaringsbehovene
 */
interface AvklaringsbehovOperasjonerRepository {
    fun hent(behandlingId: BehandlingId): List<Avklaringsbehov>
    fun opprett(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        funnetISteg: StegType,
        frist: LocalDate? = null,
        begrunnelse: String = "",
        endretAv: String = SYSTEMBRUKER.ident
    )

    fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean)
    fun endre(avklaringsbehovId: Long, endring: Endring)
    fun endreVentepunkt(avklaringsbehovId: Long, endring: Endring, funnetISteg: StegType)
}