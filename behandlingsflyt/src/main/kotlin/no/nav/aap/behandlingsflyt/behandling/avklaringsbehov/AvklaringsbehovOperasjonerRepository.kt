package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

/**
 * Kun for bruk innad i Avklaringsbehovene
 */
interface AvklaringsbehovOperasjonerRepository : Repository {
    fun hent(behandlingId: BehandlingId): List<Avklaringsbehov>
    fun hentAlleAvklaringsbehovForSak(behandlingIder: List<BehandlingId>): List<AvklaringsbehovForSak>
    fun opprett(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        funnetISteg: StegType,
        frist: LocalDate? = null,
        begrunnelse: String = "",
        grunn: ÅrsakTilSettPåVent? = null,
        endretAv: String = SYSTEMBRUKER.ident,
        perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>? = null,
    )

    fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean)
    fun endre(avklaringsbehovId: Long, endring: Endring)
    fun endreVentepunkt(avklaringsbehovId: Long, endring: Endring, funnetISteg: StegType)
    fun endreSkrivBrev(avklaringsbehovId: Long, endring: Endring, funnetISteg: StegType)
}