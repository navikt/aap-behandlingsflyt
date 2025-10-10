package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeInformasjonskrav.YrkesskadeRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeRegisterGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Søknad
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class YrkesskadeInformasjonskrav private constructor(
    private val sakService: SakService,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val yrkesskadeRegisterGateway: YrkesskadeRegisterGateway,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav<YrkesskadeInformasjonskrav.YrkesskadeInput, YrkesskadeRegisterdata> {
    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering()
                && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
                && (oppdatert.ikkeKjørtSisteKalenderdag() || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode)
    }

    data class YrkesskadeInput(val person: Person, val fødselsdato: Fødselsdato) : InformasjonskravInput

    data class YrkesskadeRegisterdata(val yrkesskader: List<Yrkesskade>) : InformasjonskravRegisterdata

    override fun klargjør(kontekst: FlytKontekstMedPerioder): YrkesskadeInput {
        val sak = sakService.hent(kontekst.sakId)
        val fødselsdato =
            requireNotNull(personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(kontekst.behandlingId)?.fødselsdato)

        return YrkesskadeInput(person = sak.person, fødselsdato = fødselsdato)
    }

    override fun hentData(input: YrkesskadeInput): YrkesskadeRegisterdata {
        val (person, fødselsdato) = input
        val registerYrkesskade: List<Yrkesskade> = yrkesskadeRegisterGateway.innhent(person, fødselsdato)

        return YrkesskadeRegisterdata(yrkesskader = registerYrkesskade)
    }

    override fun oppdater(
        input: YrkesskadeInput,
        registerdata: YrkesskadeRegisterdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)

        val registerYrkesskade: List<Yrkesskade> = registerdata.yrkesskader
        val oppgittYrkesskade = oppgittYrkesskade(kontekst.sakId, sak.rettighetsperiode)
        val oppgittYrkesskadeUtenSkadedato = oppgittYrkesskade(kontekst.sakId, null)
        val yrkesskader = registerYrkesskade + listOfNotNull(oppgittYrkesskade, oppgittYrkesskadeUtenSkadedato)

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
        periode: Periode?,
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
            val yrkesskadeString = when (val data = dokument.strukturerteData<Søknad>()?.data) {
                is SøknadV0 -> data.yrkesskade.uppercase()
                null -> error("Søknad kan ikke være null")
            }
            yrkesskadeString == "JA"
        }
    }

    private fun fakeOppgittYrkesskade(
        periode: Periode?
    ): Yrkesskade {
        check(Miljø.er() in listOf(MiljøKode.DEV, MiljøKode.LOKALT))
        check(!Miljø.erProd())

        val skadedato = if (periode != null) {
            periode.fom.minusDays(60)
        } else {
            null
        }
        return Yrkesskade(
            ref = "YRK" + "-" + Math.floor(Math.random() * 100),
            saksnummer = null,
            kildesystem = "KELVIN",
            skadedato = skadedato,
        )
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.YRKESSKADE

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): YrkesskadeInformasjonskrav {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val personopplysningRepository =
                repositoryProvider.provide<PersonopplysningRepository>()
            val mottattDokumentRepository =
                repositoryProvider.provide<MottattDokumentRepository>()
            return YrkesskadeInformasjonskrav(
                SakService(sakRepository),
                repositoryProvider.provide(),
                personopplysningRepository,
                gatewayProvider.provide(),
                mottattDokumentRepository,
                TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}
