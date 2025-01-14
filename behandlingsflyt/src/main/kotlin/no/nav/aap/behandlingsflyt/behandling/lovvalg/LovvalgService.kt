package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class LovvalgService private constructor(
    private val medlemskapGateway: MedlemskapGateway,
    private val pdlGateway: IdentGateway,
    private val sakService: SakService,
): Informasjonskrav {
    private val NORGE = "NOR"

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)


        return IKKE_ENDRET
    }



    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            // Skal alltid innhentes
            return true
        }

        override fun konstruer(connection: DBConnection): LovvalgService {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide(SakRepository::class)
            val identGateway = GatewayProvider.provide(IdentGateway::class)
            return LovvalgService(
                MedlemskapGateway(),
                identGateway,
                SakService(sakRepository)
            )
        }
    }
}