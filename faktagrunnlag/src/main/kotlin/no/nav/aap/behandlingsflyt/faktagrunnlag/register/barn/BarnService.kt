package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.PdlBarnGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BarnService private constructor(
    private val sakService: SakService,
    private val barnRepository: BarnRepository,
    private val barnGateway: BarnGateway
) : Informasjonskrav {

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val behandlingId = kontekst.behandlingId

        val barn = if (harBehandlingsgrunnlag(behandlingId)) {
            val sak = sakService.hent(kontekst.sakId)
            barnGateway.hentBarn(sak.person)
        } else {
            emptyList()
        }

        val eksisterendeData = barnRepository.hent(behandlingId)
        if (barn.toSet() != eksisterendeData.barn.toSet()) {
            barnRepository.lagre(behandlingId, barn)
            return false
        }
        return true
    }

    private fun harBehandlingsgrunnlag(behandlingId: BehandlingId): Boolean {
        // TODO: Avgjøre om man har hjemmel til å innhente (dvs er det innvilget)
        return true
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): BarnService {
            return BarnService(
                SakService(connection),
                BarnRepository(connection),
                PdlBarnGateway
            )
        }
    }
}
