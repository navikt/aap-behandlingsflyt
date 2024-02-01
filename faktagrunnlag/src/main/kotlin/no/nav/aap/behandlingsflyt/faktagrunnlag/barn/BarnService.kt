package no.nav.aap.behandlingsflyt.faktagrunnlag.barn

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.barn.adapter.BarnRelasjonerMock
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService

class BarnService private constructor(connection: DBConnection) : Grunnlag {

    private val barnRepository = BarnRepository(connection)
    private val sakService = SakService(connection)

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val sak = sakService.hent(kontekst.sakId)
        val behandlingId = kontekst.behandlingId

        val harBehandlingsgrunnlag = vurderBehandlingsgrunnlag(behandlingId)

        val barn = if (harBehandlingsgrunnlag) {
            BarnRelasjonerMock.innhent(sak.person.identer(), sak.rettighetsperiode)
        } else {
            listOf()
        }

        val gamleData = barnRepository.hentHvisEksisterer(behandlingId)

        barnRepository.lagre(behandlingId, barn)
        val nyeData = barnRepository.hentHvisEksisterer(behandlingId)

        return nyeData == gamleData
    }

    private fun vurderBehandlingsgrunnlag(behandlingId: BehandlingId): Boolean {
        // TODO: Avgjøre om man har hjemmel til å innhente (dvs er det innvilget)
        return true
    }

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): BarnService {
            return BarnService(connection)
        }
    }
}
