package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

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
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Duration
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGateway as IInstitusjonsoppholdGateway

class InstitusjonsoppholdService private constructor(
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val institusjonsoppholdRegisterGateway: IInstitusjonsoppholdGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav, KanTriggeRevurdering {
    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val eksisterendeGrunnlag = hentHvisEksisterer(kontekst.behandlingId)
        val institusjonsopphold = hentInstitusjonsopphold(kontekst.behandlingId)

        institusjonsoppholdRepository.lagreOpphold(kontekst.behandlingId, institusjonsopphold)

        return if (erEndret(eksisterendeGrunnlag, institusjonsopphold)) ENDRET else IKKE_ENDRET
    }

    private fun hentInstitusjonsopphold(behandlingId: BehandlingId): List<Institusjonsopphold> {
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        return institusjonsoppholdRegisterGateway.innhent(sak.person)
            .filter { it.periode().overlapper(sak.rettighetsperiode) }
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag? {
        return institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)
    }

    override fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val institusjonsopphold = hentInstitusjonsopphold(behandlingId)

        return if (erEndret(eksisterendeGrunnlag, institusjonsopphold))
            listOf(VurderingsbehovMedPeriode(Vurderingsbehov.INSTITUSJONSOPPHOLD))
        else
            emptyList()
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.INSTITUSJONSOPPHOLD

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): InstitusjonsoppholdService {
            return InstitusjonsoppholdService(
                SakOgBehandlingService(repositoryProvider, gatewayProvider),
                repositoryProvider.provide(),
                gatewayProvider.provide(),
                TidligereVurderingerImpl(repositoryProvider),
            )
        }


        fun erEndret(
            eksisterendeGrunnlag: InstitusjonsoppholdGrunnlag?,
            institusjonsopphold: List<Institusjonsopphold>
        ): Boolean {
            val oppholdeneFraRegister = Oppholdene(null, institusjonsopphold.map { it.tilInstitusjonSegment() })
            return eksisterendeGrunnlag == null || eksisterendeGrunnlag.oppholdene != oppholdeneFraRegister
        }
    }
}