package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdagForBehandling
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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class YrkesskadeInformasjonskrav internal constructor(
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
                && (oppdatert.ikkeKjørtSisteKalenderdagForBehandling(kontekst.behandlingId) || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode)
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

        val registerYrkesskader: List<Yrkesskade> = registerdata.yrkesskader
        val mottattDokumenter = mottattDokumentRepository.hentDokumenterAvType(kontekst.sakId, InnsendingType.SØKNAD)
        val harOppgittYrkesskade = harOppgittYrkesskade(mottattDokumenter)

        val behandlingId = kontekst.behandlingId
        val gamleData = yrkesskadeRepository.hentHvisEksisterer(behandlingId)

        yrkesskadeRepository.lagre(
            behandlingId,
            registerYrkesskader = Yrkesskader(registerYrkesskader).takeIf { it.yrkesskader.isNotEmpty() },
            oppgittYrkesskadeISøknad = harOppgittYrkesskade,
        )

        val nyeData = yrkesskadeRepository.hentHvisEksisterer(behandlingId)

        return if (nyeData == gamleData) IKKE_ENDRET else ENDRET
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

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.YRKESSKADE

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): YrkesskadeInformasjonskrav {
            val personopplysningRepository =
                repositoryProvider.provide<PersonopplysningRepository>()
            val mottattDokumentRepository =
                repositoryProvider.provide<MottattDokumentRepository>()
            return YrkesskadeInformasjonskrav(
                SakService(repositoryProvider, gatewayProvider),
                repositoryProvider.provide(),
                personopplysningRepository,
                gatewayProvider.provide(),
                mottattDokumentRepository,
                TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
            )
        }
    }
}
