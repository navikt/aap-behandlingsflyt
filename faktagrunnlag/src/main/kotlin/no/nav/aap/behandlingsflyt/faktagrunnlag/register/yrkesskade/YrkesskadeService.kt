package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeRegisterGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class YrkesskadeService private constructor(
    private val sakService: SakService,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val yrkesskadeRegisterGateway: YrkesskadeRegisterGateway
) : Informasjonskrav {

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val fødselsdato =
            requireNotNull(personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)?.brukerPersonopplysning?.fødselsdato)
        val yrkesskadePeriode = yrkesskadeRegisterGateway.innhent(sak.person, fødselsdato)

        val behandlingId = kontekst.behandlingId
        val gamleData = yrkesskadeRepository.hentHvisEksisterer(behandlingId)

        if (yrkesskadePeriode.isNotEmpty()) {
            yrkesskadeRepository.lagre(
                behandlingId,
                Yrkesskader(yrkesskadePeriode.map { skade -> Yrkesskade(skade.ref, skade.skadedato) })
            )
        } else if (yrkesskadeRepository.hentHvisEksisterer(behandlingId) != null) {
            yrkesskadeRepository.lagre(behandlingId, null)
        }
        val nyeData = yrkesskadeRepository.hentHvisEksisterer(behandlingId)

        return if (nyeData == gamleData) IKKE_ENDRET else ENDRET
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? {
        return yrkesskadeRepository.hentHvisEksisterer(behandlingId)
    }

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            // Skal kun innhente når 11-5 skal vurderes
            return true
        }
        override fun konstruer(connection: DBConnection): YrkesskadeService {
            return YrkesskadeService(
                SakService(SakRepositoryImpl(connection)),
                YrkesskadeRepository(connection),
                PersonopplysningRepository(connection),
                YrkesskadeRegisterGateway
            )
        }
    }
}
