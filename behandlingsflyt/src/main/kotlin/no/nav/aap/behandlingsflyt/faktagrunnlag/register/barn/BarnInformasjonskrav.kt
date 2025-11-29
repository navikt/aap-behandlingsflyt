package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

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
import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId

class BarnInformasjonskrav private constructor(
    private val barnRepository: BarnRepository,
    private val personRepository: PersonRepository,
    private val barnGateway: BarnGateway,
    private val identGateway: IdentGateway,
    private val tidligereVurderinger: TidligereVurderinger,
    private val sakService: SakService,
    private val unleashGateway: UnleashGateway
) : Informasjonskrav<BarnInformasjonskrav.BarnInput, BarnInformasjonskrav.Registerdata>, KanTriggeRevurdering {

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
        return if (unleashGateway.isEnabled(BehandlingsflytFeature.NyeBarn)) {
            gyldigBehandling &&
                    (oppdatert == null || oppdatert.oppdatert.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime()
                        .isBefore(LocalDateTime.now().minusHours(1))) &&
                    !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
        } else {
            gyldigBehandling &&
                    oppdatert.ikkeKjørtSisteKalenderdag() &&
                    !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
        }
    }

    data class BarnInput(
        val barnGrunnlag: BarnGrunnlag?,
        val person: Person
    ) : InformasjonskravInput

    data class Registerdata(
        val barnInnhentingRespons: BarnInnhentingRespons,
        val identlisteForBarn: Map<BarnIdentifikator.BarnIdent, List<Ident>>
    ) : InformasjonskravRegisterdata

    override fun klargjør(kontekst: FlytKontekstMedPerioder): BarnInput {
        val behandlingId = kontekst.behandlingId
        val barnGrunnlag = barnRepository.hentHvisEksisterer(behandlingId)
        val sak = sakService.hentSakFor(behandlingId)
        val person = sak.person

        return BarnInput(
            barnGrunnlag = barnGrunnlag,
            person = person
        )
    }

    override fun hentData(input: BarnInput): Registerdata {
        val (barnGrunnlag, person) = input
        val saksbehandlerOppgitteBarnIdenter = barnGrunnlag?.saksbehandlerOppgitteBarn?.barn?.mapNotNull { it.ident }.orEmpty()
        val oppgitteBarnIdenter = barnGrunnlag?.oppgitteBarn?.oppgitteBarn?.mapNotNull { it.ident }.orEmpty()
        val barn = barnGateway.hentBarn(person, oppgitteBarnIdenter, saksbehandlerOppgitteBarnIdenter)

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

    override fun oppdater(
        input: BarnInput,
        registerdata: Registerdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        log.info("Oppdaterer barnegrunnlag for behandling ${kontekst.behandlingId} av type ${kontekst.behandlingType} med årsak(er) ${kontekst.vurderingsbehovRelevanteForSteg}")
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
        val saksbehandlerOppgitteBarn = barnGrunnlag?.saksbehandlerOppgitteBarn?.barn?.mapNotNull { it.ident }.orEmpty()
        val oppgitteBarnIdenter = barnGrunnlag?.oppgitteBarn?.oppgitteBarn?.mapNotNull { it.ident }.orEmpty()
        val barn = barnGateway.hentBarn(person, oppgitteBarnIdenter, saksbehandlerOppgitteBarn)
        return barn
    }

    private fun harEndringer(barnGrunnlag: BarnGrunnlag?, registerBarn: List<Barn>): Boolean {
        return registerBarn.toSet() != barnGrunnlag?.registerbarn?.barn?.toSet()
    }

    override fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        val barnGrunnlag = barnRepository.hentHvisEksisterer(behandlingId)
        val sak = sakService.hentSakFor(behandlingId)
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
        ): Informasjonskrav<BarnInput, Registerdata> {
            return BarnInformasjonskrav(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                gatewayProvider.provide(),
                gatewayProvider.provide(),
                TidligereVurderingerImpl(repositoryProvider),
                SakService(repositoryProvider),
                gatewayProvider.provide<UnleashGateway>()
            )
        }
    }
}
