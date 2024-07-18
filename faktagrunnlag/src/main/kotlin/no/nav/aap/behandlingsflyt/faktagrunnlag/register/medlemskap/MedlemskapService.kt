package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapGateway

class MedlemskapService private constructor(
    private val medlemskapGateway: MedlemskapGateway,
    private val sakService: SakService
) : Informasjonskrav {
    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val sak = sakService.hent(kontekst.sakId)
        val medlemskapPeriode = medlemskapGateway.innhent(sak.person)

        TODO("Not yet implemented")
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): MedlemskapService {
            return MedlemskapService(
                MedlemskapGateway(),
                SakService(connection)
            )
        }
    }
}