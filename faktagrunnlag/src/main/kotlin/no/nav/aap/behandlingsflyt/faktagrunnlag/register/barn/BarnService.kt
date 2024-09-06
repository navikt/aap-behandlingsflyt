package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BarnInnhentingRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.PdlBarnGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.RelatertPersonopplysning
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnService private constructor(
    private val sakService: SakService,
    private val barnRepository: BarnRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val personRepository: PersonRepository,
    private val barnGateway: BarnGateway,
    private val pdlGateway: IdentGateway,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) : Informasjonskrav {

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val behandlingId = kontekst.behandlingId
        val eksisterendeData = barnRepository.hentHvisEksisterer(behandlingId)

        val oppgitteIdenter = eksisterendeData?.oppgitteBarn?.identer?.toList() ?: emptyList()
        val barn = if (harBehandlingsgrunnlag(behandlingId)) {
            val sak = sakService.hent(kontekst.sakId)
            barnGateway.hentBarn(sak.person, oppgitteIdenter)
        } else {
            BarnInnhentingRespons(emptyList(), emptyList())
        }

        val relatertePersonopplysninger =
            personopplysningRepository.hentHvisEksisterer(behandlingId)?.relatertePersonopplysninger?.personopplysninger
        val barnIdenter = barn.registerBarn.map { it.ident }.toSet()

        oppdaterPersonIdenter(barn.alleBarn().map { it.ident }.toSet())

        if (harEndringerIIdenter(barnIdenter, eksisterendeData)
            || harEndringerIPersonopplysninger(barn.alleBarn(), relatertePersonopplysninger)
        ) {
            barnRepository.lagreRegisterBarn(behandlingId, barnIdenter)
            personopplysningRepository.lagre(behandlingId, barn.alleBarn())
            return false
        }
        return true
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
        barn: List<Barn>,
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

    private fun harBehandlingsgrunnlag(behandlingId: BehandlingId): Boolean {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        return sykdomsvilkåret.harPerioderSomErOppfylt() && bistandsvilkåret.harPerioderSomErOppfylt()
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): BarnService {
            return BarnService(
                SakService(connection),
                BarnRepository(connection),
                PersonopplysningRepository(connection),
                PersonRepository(connection),
                PdlBarnGateway,
                PdlIdentGateway,
                VilkårsresultatRepository(connection)
            )
        }
    }
}
