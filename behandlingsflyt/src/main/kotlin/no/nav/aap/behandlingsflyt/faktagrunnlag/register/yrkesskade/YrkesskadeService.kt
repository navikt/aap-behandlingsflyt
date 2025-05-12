package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeRegisterGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Søknad
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Duration

class YrkesskadeService private constructor(
    private val sakService: SakService,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val yrkesskadeRegisterGateway: YrkesskadeRegisterGateway,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav {
    override val navn = Companion.navn

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                tidligereVurderinger.harBehandlingsgrunnlag(kontekst, steg)
    }


    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val fødselsdato =
            requireNotNull(personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)?.brukerPersonopplysning?.fødselsdato)
        val registerYrkesskade: List<Yrkesskade> = yrkesskadeRegisterGateway.innhent(sak.person, fødselsdato)
        val oppgittYrkesskade = oppgittYrkesskade(kontekst.sakId, sak.rettighetsperiode)
        val yrkesskader = registerYrkesskade + listOfNotNull(oppgittYrkesskade)

        val behandlingId = kontekst.behandlingId
        val gamleData = yrkesskadeRepository.hentHvisEksisterer(behandlingId)

        if (yrkesskader.isNotEmpty()) {
            yrkesskadeRepository.lagre(
                behandlingId,
                Yrkesskader(yrkesskader)
            )
        } else if (yrkesskadeRepository.hentHvisEksisterer(behandlingId) != null) {
            yrkesskadeRepository.lagre(behandlingId, null)
        }
        val nyeData = yrkesskadeRepository.hentHvisEksisterer(behandlingId)

        return if (nyeData == gamleData) IKKE_ENDRET else ENDRET
    }

    private fun oppgittYrkesskade(
        id: SakId,
        periode: Periode,
    ): Yrkesskade? {
        val mottattDokumenter = mottattDokumentRepository.hentDokumenterAvType(id, InnsendingType.SØKNAD)

        if (harOppgittYrkesskade(mottattDokumenter)) {
            if (Miljø.er() in listOf(MiljøKode.DEV, MiljøKode.LOKALT)) {
                return fakeOppgittYrkesskade(periode)
            }

            /* TODO: Modeller og vis informasjon fra søknad i kelvin. */
        }

        return null
    }

    private fun harOppgittYrkesskade(mottattDokumenter: Set<MottattDokument>): Boolean {
        return mottattDokumenter.any { dokument ->
            val data = dokument.strukturerteData<Søknad>()?.data
            val yrkesskadeString = when (data) {
                is SøknadV0 -> data.yrkesskade.uppercase()
                null -> error("Søknad kan ikke være null")
            }
            yrkesskadeString == "JA"
        }
    }

    private fun fakeOppgittYrkesskade(
        periode: Periode
    ): Yrkesskade {
        check(Miljø.er() in listOf(MiljøKode.DEV, MiljøKode.LOKALT))
        check(!Miljø.erProd())

        return Yrkesskade(
            ref = "YRK",
            skadedato = periode.fom.minusDays(60),
        )
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? {
        return yrkesskadeRepository.hentHvisEksisterer(behandlingId)
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.YRKESSKADE

        override fun konstruer(repositoryProvider: RepositoryProvider): YrkesskadeService {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val personopplysningRepository =
                repositoryProvider.provide<PersonopplysningRepository>()
            val mottattDokumentRepository =
                repositoryProvider.provide<MottattDokumentRepository>()
            return YrkesskadeService(
                SakService(sakRepository),
                repositoryProvider.provide(),
                personopplysningRepository,
                GatewayProvider.provide(),
                mottattDokumentRepository,
                TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}
