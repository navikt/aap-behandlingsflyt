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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Duration

class BarnService private constructor(
    private val sakService: SakService,
    private val barnRepository: BarnRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val personRepository: PersonRepository,
    private val barnGateway: BarnGateway,
    private val pdlGateway: IdentGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav {

    override val navn = Companion.navn

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                tidligereVurderinger.harBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val behandlingId = kontekst.behandlingId
        val eksisterendeData = barnRepository.hentHvisEksisterer(behandlingId)

        val oppgitteIdenter = eksisterendeData?.oppgitteBarn?.identer?.toList() ?: emptyList()
        val sak = sakService.hent(kontekst.sakId)
        val barn = barnGateway.hentBarn(sak.person, oppgitteIdenter)

        val relatertePersonopplysninger =
            personopplysningRepository.hentHvisEksisterer(behandlingId)?.relatertePersonopplysninger?.personopplysninger
        val barnIdenter = barn.registerBarn.map { it.ident }.toSet()

        oppdaterPersonIdenter(barn.alleBarn().map { it.ident }.toSet())

        if (harEndringerIIdenter(barnIdenter, eksisterendeData)
            || harEndringerIPersonopplysninger(barn.alleBarn(), relatertePersonopplysninger)
        ) {
            barnRepository.lagreRegisterBarn(behandlingId, barnIdenter)
            personopplysningRepository.lagre(behandlingId, barn.alleBarn())
            return ENDRET
        }
        return IKKE_ENDRET
    }

    private fun oppdaterPersonIdenter(barnIdenter: Set<Ident>) {
        barnIdenter.forEach { ident ->
            val identliste = pdlGateway.hentAlleIdenterForPerson(ident)
            if (identliste.isEmpty()) {
                throw IllegalStateException("Fikk ingen treff på ident i PDL")
            }

            personRepository.finnEllerOpprett(identliste)
        }
    }

    private fun harEndringerIPersonopplysninger(
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
        }?.toSet() ?: setOf()

        return barn.toSet() != eksisterendeData
    }

    private fun harEndringerIIdenter(
        barnIdenter: Set<Ident>,
        eksisterendeData: BarnGrunnlag?
    ): Boolean {
        return barnIdenter != eksisterendeData?.registerbarn?.identer?.toSet()
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
            return BarnService(
                SakService(sakRepository),
                repositoryProvider.provide(),
                personopplysningRepository,
                personRepository,
                barnGateway,
                identGateway,
                TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}
