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
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.RelatertPersonopplysning
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling.BARNETILLEGG
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
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
    private val personopplysningRepository: PersonopplysningRepository,
    private val personRepository: PersonRepository,
    private val barnGateway: BarnGateway,
    private val pdlGateway: IdentGateway,
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
        val gyldigBehandling = kontekst.erFørstegangsbehandling() || kontekst.erRevurderingMedÅrsak(BARNETILLEGG)
        return gyldigBehandling &&
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        log.info("Oppdaterer barnegrunnlag for behandling ${kontekst.behandlingId} av type ${kontekst.behandlingType} med årsak(er) ${kontekst.årsakerTilBehandling}")

        val behandlingId = kontekst.behandlingId
        val sak = sakService.hent(kontekst.sakId)
        val barnGrunnlag = barnRepository.hentHvisEksisterer(behandlingId)

        val oppgitteBarnIdenter = barnGrunnlag?.oppgitteBarn?.oppgitteBarn?.mapNotNull { it.ident }.orEmpty()
        val barn = barnGateway.hentBarn(sak.person, oppgitteBarnIdenter)
        val registerBarnIdenter = barn.registerBarn.map { it.ident }

        val relatertePersonopplysninger =
            personopplysningRepository.hentHvisEksisterer(behandlingId)?.relatertePersonopplysninger?.personopplysninger

        oppdaterPersonIdenter(barn.alleBarn().map { it.ident }.toSet())

        val manglerBarnGrunnlagEllerFantNyeBarnFraRegister =
            manglerBarnGrunnlagEllerFantNyeBarnFraRegister(registerBarnIdenter, barnGrunnlag)
        val personopplysningerForBarnErOppdatert =
            personopplysningerForBarnErOppdatert(barn.alleBarn(), relatertePersonopplysninger)

        if (manglerBarnGrunnlagEllerFantNyeBarnFraRegister || personopplysningerForBarnErOppdatert) {
            barnRepository.lagreRegisterBarn(behandlingId, registerBarnIdenter)
            personopplysningRepository.lagre(behandlingId, barn.alleBarn())
            return ENDRET
        }
        return IKKE_ENDRET
    }

    private fun oppdaterPersonIdenter(barnIdenter: Set<Ident>) {
        barnIdenter.forEach { ident ->
            val identliste = pdlGateway.hentAlleIdenterForPerson(ident)
            if (identliste.isEmpty()) {
                throw IllegalStateException("Fikk ingen treff på ident i PDL.")
            }

            personRepository.finnEllerOpprett(identliste)
        }
    }

    private fun personopplysningerForBarnErOppdatert(
        barn: Set<Barn>,
        relatertePersonopplysninger: List<RelatertPersonopplysning>?
    ): Boolean {
        if (barn.isNotEmpty() && relatertePersonopplysninger.isNullOrEmpty()) {
            return true
        }
        val eksisterendeData = relatertePersonopplysninger?.map { opplysning ->
            Barn(
                opplysning.ident(),
                opplysning.fødselsdato,
                opplysning.dødsdato
            )
        }.orEmpty().toSet()

        return barn.toSet() != eksisterendeData
    }

    private fun manglerBarnGrunnlagEllerFantNyeBarnFraRegister(
        barnIdenter: List<Ident>,
        barnGrunnlag: BarnGrunnlag?
    ): Boolean {
        return barnIdenter.toSet() != barnGrunnlag?.registerbarn?.identer?.toSet()
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.BARN

        override fun konstruer(repositoryProvider: RepositoryProvider): BarnService {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val personRepository = repositoryProvider.provide<PersonRepository>()
            val personopplysningRepository =
                repositoryProvider.provide<PersonopplysningRepository>()
            val barnGateway = GatewayProvider.provide(BarnGateway::class)
            val identGateway = GatewayProvider.provide(IdentGateway::class)
            val unleashGateway = GatewayProvider.provide(UnleashGateway::class)
            return BarnService(
                SakService(sakRepository),
                repositoryProvider.provide(),
                personopplysningRepository,
                personRepository,
                barnGateway,
                identGateway,
                TidligereVurderingerImpl(repositoryProvider),
                unleashGateway,
            )
        }
    }
}
