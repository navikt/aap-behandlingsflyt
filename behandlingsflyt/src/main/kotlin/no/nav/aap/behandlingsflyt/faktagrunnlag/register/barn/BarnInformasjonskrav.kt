package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BarnInnhentingRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.BARNETILLEGG
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class BarnInformasjonskrav private constructor(
    private val barnRepository: BarnRepository,
    private val personRepository: PersonRepository,
    private val barnGateway: BarnGateway,
    private val identGateway: IdentGateway,
    private val tidligereVurderinger: TidligereVurderinger,
    private val sakOgBehandlingService: SakOgBehandlingService
) : Informasjonskrav, KanTriggeRevurdering {

    private val log = LoggerFactory.getLogger(javaClass)

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        // Kun gjøre oppslag mot register ved førstegangsbehandling og revurdering av barnetillegg (Se AAP-933).
        val gyldigBehandling =
            kontekst.erFørstegangsbehandling() || kontekst.erRevurderingMedVurderingsbehov(BARNETILLEGG)
        return gyldigBehandling &&
                oppdatert.ikkeKjørtSisteKalenderdag() &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    data class BarnInput(
        val barnGrunnlag: BarnGrunnlag?,
        val person: Person,
    )

    data class Registerdata(
        val barnInnhentingRespons: BarnInnhentingRespons,
        val identlisteForBarn: Map<BarnIdentifikator.BarnIdent, List<Ident>>
    )

    fun hentInput(kontekst: FlytKontekstMedPerioder): BarnInput {
        val behandlingId = kontekst.behandlingId
        val barnGrunnlag = barnRepository.hentHvisEksisterer(behandlingId)
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        val person = sak.person

        return BarnInput(
            barnGrunnlag = barnGrunnlag,
            person = person
        )
    }

    fun hentData(input: BarnInput): Registerdata {
        val (barnGrunnlag, person) = input
        val oppgitteBarnIdenter = barnGrunnlag?.oppgitteBarn?.oppgitteBarn?.mapNotNull { it.ident }.orEmpty()
        val barn = barnGateway.hentBarn(person, oppgitteBarnIdenter)

        val identListeForBarn = barn.alleBarn()
            .map { it.ident }
            .filterIsInstance<BarnIdentifikator.BarnIdent>()
            .associateWith {
                identGateway.hentAlleIdenterForPerson(it.ident).also {
                    if (it.isEmpty()) {
                        throw IllegalStateException("Fikk ingen treff på ident i PDL.")
                    }
                }
            }

        return Registerdata(barnInnhentingRespons = barn, identlisteForBarn = identListeForBarn)
    }

    fun sammenlign(
        kontekst: FlytKontekstMedPerioder,
        input: BarnInput,
        registerdata: Registerdata
    ): Informasjonskrav.Endret {
        val behandlingId = kontekst.behandlingId
        val barnGrunnlag = input.barnGrunnlag
        val registerbarn = registerdata.barnInnhentingRespons.registerBarn
        val barn = registerdata.barnInnhentingRespons
        if (harEndringer(barnGrunnlag, registerbarn)) {
            val barnMedPersonId = oppdaterPersonIdenter(barn.alleBarn(), registerdata.identlisteForBarn)

            val registerBarnMedFolkeregisterRelasjon =
                barnMedPersonId.filterKeys { barn -> barn.ident in registerbarn.map { it.ident } }
            barnRepository.lagreRegisterBarn(behandlingId, registerBarnMedFolkeregisterRelasjon)
            return ENDRET
        }
        return IKKE_ENDRET
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        log.info("Oppdaterer barnegrunnlag for behandling ${kontekst.behandlingId} av type ${kontekst.behandlingType} med årsak(er) ${kontekst.vurderingsbehovRelevanteForSteg}")
        val input = hentInput(kontekst)

        val data = hentData(input)

        return sammenlign(kontekst, input, data)
    }

    private fun oppdaterPersonIdenter(
        barn: List<Barn>,
        identlisteForBarn: Map<BarnIdentifikator.BarnIdent, List<Ident>>
    ): Map<Barn, PersonId?> {
        return barn.associateWith { barn ->
            when (barn.ident) {
                is BarnIdentifikator.BarnIdent -> {
                    val identliste = identlisteForBarn[barn.ident]!!
                    personRepository.finnEllerOpprett(identliste).id
                }

                is BarnIdentifikator.NavnOgFødselsdato -> null
            }
        }
    }

    private fun hentRegisterBarn(
        barnGrunnlag: BarnGrunnlag?,
        person: Person,
    ): BarnInnhentingRespons {
        val oppgitteBarnIdenter = barnGrunnlag?.oppgitteBarn?.oppgitteBarn?.mapNotNull { it.ident }.orEmpty()
        val barn = barnGateway.hentBarn(person, oppgitteBarnIdenter)
        return barn
    }

    private fun harEndringer(barnGrunnlag: BarnGrunnlag?, registerBarn: List<Barn>): Boolean {
        return manglerBarnGrunnlagEllerFantNyeBarnFraRegister(
            registerBarn,
            barnGrunnlag?.registerbarn?.barn
        )
    }


    private fun manglerBarnGrunnlagEllerFantNyeBarnFraRegister(
        barnIdenter: List<Barn>,
        eksisterendeRegisterBarn: List<Barn>?
    ): Boolean {
        return barnIdenter.map { it.ident }.toSet() != eksisterendeRegisterBarn?.map { it.ident }?.toSet()
    }

    override fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        val barnGrunnlag = barnRepository.hentHvisEksisterer(behandlingId)
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        val registerBarn = hentRegisterBarn(barnGrunnlag, sak.person).registerBarn
        return if (!harEndringer(barnGrunnlag, registerBarn)) {
            emptyList()
        } else {
            listOf(VurderingsbehovMedPeriode(BARNETILLEGG, null))
        }
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.BARN

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BarnInformasjonskrav {
            return BarnInformasjonskrav(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                gatewayProvider.provide(),
                gatewayProvider.provide(),
                TidligereVurderingerImpl(repositoryProvider),
                SakOgBehandlingService(repositoryProvider, gatewayProvider)
            )
        }
    }
}
