package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeModell
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeRegisterGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.repository.RepositoryProvider
import java.time.LocalDate

class YrkesskadeService private constructor(
    private val sakService: SakService,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val yrkesskadeRegisterGateway: YrkesskadeRegisterGateway,
    private val mottattDokumentRepository: MottattDokumentRepositoryImpl
) : Informasjonskrav {

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val fødselsdato =
            requireNotNull(personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)?.brukerPersonopplysning?.fødselsdato)
        val oppgittYrkesskade = oppgittYrkesskade(kontekst.sakId, sak.rettighetsperiode)
        val yrkesskadePeriode = yrkesskadeRegisterGateway.innhent(sak.person, fødselsdato, oppgittYrkesskade)

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

    private fun oppgittYrkesskade(
        id: SakId,
        periode: Periode
    ): YrkesskadeModell? {
        val mottattDokumenter = mottattDokumentRepository.hentDokumenterAvType(id, InnsendingType.SØKNAD)

        if (mottattDokumenter.any { dokument -> dokument.strukturerteData<Søknad>()?.data?.harYrkesskade() == true }) {
            return YrkesskadeModell(
                kommunenr = "0301",
                saksblokk = "1",
                saksnr = 123456,
                sakstype = "YRK",
                mottattdato = LocalDate.now(),
                resultat = "I",
                resultattekst = "Innvilget",
                vedtaksdato = LocalDate.now(),
                skadeart = "YRK",
                diagnose = "YRK",
                skadedato = periode.fom.minusDays(60),
                kildetabell = "YRK",
                kildesystem = "YRK",
                saksreferanse = "YRK"
            )
        }
        return null
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
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide(SakRepository::class)
            val personopplysningRepository = repositoryProvider.provide(PersonopplysningRepository::class)
            return YrkesskadeService(
                SakService(sakRepository),
                YrkesskadeRepository(connection),
                personopplysningRepository,
                YrkesskadeRegisterGateway,
                MottattDokumentRepositoryImpl(connection),
            )
        }
    }
}
