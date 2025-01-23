package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class ForutgåendeMedlemskapService private constructor(
    private val medlemskapGateway: MedlemskapGateway,
    private val sakService: SakService,
    private val medlemskapRepository: MedlemskapRepository,
) : Informasjonskrav {
    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val behandlingId = kontekst.behandlingId
        val sak = sakService.hent(kontekst.sakId)
        val medlemskapPerioder = medlemskapGateway.innhent(sak.person, Periode(sak.rettighetsperiode.fom.minusYears(5), sak.rettighetsperiode.fom))
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        medlemskapRepository.lagreUnntakMedlemskap(kontekst.behandlingId, medlemskapPerioder)

        return if (erUendret(eksisterendeGrunnlag, hentHvisEksisterer(behandlingId))) IKKE_ENDRET else ENDRET
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapUnntakGrunnlag? {
        return medlemskapRepository.hentHvisEksisterer(behandlingId)
    }

    private fun erUendret(
        eksisterendeGrunnlag: MedlemskapUnntakGrunnlag?,
        nyttGrunnlag: MedlemskapUnntakGrunnlag?
    ): Boolean {
        return eksisterendeGrunnlag?.unntak == nyttGrunnlag?.unntak
    }

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            return true
        }

        override fun konstruer(connection: DBConnection): ForutgåendeMedlemskapService {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            return ForutgåendeMedlemskapService(
                MedlemskapGateway(),
                SakService(sakRepository),
                MedlemskapRepository(connection)
            )
        }
    }
}