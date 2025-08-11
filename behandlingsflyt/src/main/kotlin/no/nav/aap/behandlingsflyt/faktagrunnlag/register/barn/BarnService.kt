package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.BARNETILLEGG
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Duration

class BarnService private constructor(
    private val sakService: SakService,
    private val barnRepository: BarnRepository,
    private val personRepository: PersonRepository,
    private val barnGateway: BarnGateway,
    private val identGateway: IdentGateway,
    private val tidligereVurderinger: TidligereVurderinger,
    private val unleashGateway: UnleashGateway
) : Informasjonskrav {

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
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        log.info("Oppdaterer barnegrunnlag for behandling ${kontekst.behandlingId} av type ${kontekst.behandlingType} med årsak(er) ${kontekst.vurderingsbehovRelevanteForSteg}")

        val behandlingId = kontekst.behandlingId
        val sak = sakService.hent(kontekst.sakId)
        val barnGrunnlag = barnRepository.hentHvisEksisterer(behandlingId)

        val oppgitteBarnIdenter = barnGrunnlag?.oppgitteBarn?.oppgitteBarn?.mapNotNull { it.ident }.orEmpty()
        val barn = barnGateway.hentBarn(sak.person, oppgitteBarnIdenter)
        val registerBarn = barn.registerBarn

        val manglerBarnGrunnlagEllerFantNyeBarnFraRegister =
            manglerBarnGrunnlagEllerFantNyeBarnFraRegister(
                registerBarn,
                barnGrunnlag?.registerbarn?.barn
            )

        if (manglerBarnGrunnlagEllerFantNyeBarnFraRegister) {
            val barnMedPersonId = oppdaterPersonIdenter(barn.alleBarn())

            val registerBarnMedFolkeregisterRelasjon =
                barnMedPersonId.filterKeys { barn -> barn.ident in registerBarn.map { it.ident } }
            barnRepository.lagreRegisterBarn(behandlingId, registerBarnMedFolkeregisterRelasjon)
            return ENDRET
        }
        return IKKE_ENDRET
    }

    private fun oppdaterPersonIdenter(barn: List<Barn>): Map<Barn, PersonId> {
        return barn.associateWith { barn ->
            val identliste = identGateway.hentAlleIdenterForPerson(barn.ident)
            if (identliste.isEmpty()) {
                throw IllegalStateException("Fikk ingen treff på ident i PDL.")
            }

            personRepository.finnEllerOpprett(identliste).id
        }
    }

    private fun manglerBarnGrunnlagEllerFantNyeBarnFraRegister(
        barnIdenter: List<Barn>,
        eksisterendeRegisterBarn: List<Barn>?
    ): Boolean {
        return barnIdenter.map { it.ident }.toSet() != eksisterendeRegisterBarn?.map { it.ident }?.toSet()
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.BARN

        override fun konstruer(repositoryProvider: RepositoryProvider): BarnService {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val barnGateway = GatewayProvider.provide(BarnGateway::class)
            val unleashGateway = GatewayProvider.provide(UnleashGateway::class)
            return BarnService(
                SakService(sakRepository),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                barnGateway,
                GatewayProvider.provide(),
                TidligereVurderingerImpl(repositoryProvider),
                unleashGateway,
            )
        }
    }
}
