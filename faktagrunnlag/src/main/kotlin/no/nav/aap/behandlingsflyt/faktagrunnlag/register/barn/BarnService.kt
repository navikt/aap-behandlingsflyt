package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.PdlBarnGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BarnService private constructor(
    private val connection: DBConnection,
    private val barnGateway: BarnGateway
) : Grunnlag {

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val barnRepository = BarnRepository(connection)
        val sakService = SakService(connection)

        val sak = sakService.hent(kontekst.sakId)
        val behandlingId = kontekst.behandlingId

        val harBehandlingsgrunnlag = vurderBehandlingsgrunnlag(behandlingId)
        val eksisterendeData = barnRepository.hent(behandlingId)
        val barn = if (harBehandlingsgrunnlag) {
            barnGateway.hentBarn(sak.person)
        } else {
            emptyList()
        }

        if (barn.toSet() != eksisterendeData.barn.toSet()) {
            barnRepository.lagre(behandlingId, barn)
            return true
        }
        return false
    }

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): BarnService {
            return BarnService(
                connection,
                PdlBarnGateway
            )
        }
    }

    private fun vurderBehandlingsgrunnlag(behandlingId: BehandlingId): Boolean {
        // TODO: Avgjøre om man har hjemmel til å innhente (dvs er det innvilget)
        return true
    }
}
