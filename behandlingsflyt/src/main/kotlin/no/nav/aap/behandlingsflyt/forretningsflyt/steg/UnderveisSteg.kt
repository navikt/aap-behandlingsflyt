package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.underveis.UnderveisService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class UnderveisSteg(private val underveisService: UnderveisService) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider): this(
        underveisService = UnderveisService(repositoryProvider),
    )
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        // Skal alltid kjøres uavhengig av vurderingstype
        underveisService.vurder(kontekst.sakId, kontekst.behandlingId)
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return UnderveisSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_UTTAK
        }
    }
}