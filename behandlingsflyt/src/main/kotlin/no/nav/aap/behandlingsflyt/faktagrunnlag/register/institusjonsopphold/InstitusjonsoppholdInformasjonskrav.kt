package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

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
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGateway as IInstitusjonsoppholdGateway

class InstitusjonsoppholdInformasjonskrav private constructor(
    private val sakService: SakService,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val institusjonsoppholdRegisterGateway: IInstitusjonsoppholdGateway,
    private val tidligereVurderinger: TidligereVurderinger,
    private val unleashGateway: UnleashGateway
) : Informasjonskrav<InstitusjonsoppholdInformasjonskrav.Input, InstitusjonsoppholdInformasjonskrav.InstitusjonsoppholdRegisterdata>,
    KanTriggeRevurdering {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        if (unleashGateway.isEnabled(BehandlingsflytFeature.HentingAvInstitusjonsOpphold))
        {
            return kontekst.erFørstegangsbehandlingEllerRevurdering()
                    && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
                    && (kontekst.rettighetsperiode != oppdatert?.rettighetsperiode)
        }
        else {
            return kontekst.erFørstegangsbehandlingEllerRevurdering()
                    && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
                    && (oppdatert.ikkeKjørtSisteKalenderdag() || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode)
        }

    }

    data class Input(val sak: Sak) : InformasjonskravInput

    data class InstitusjonsoppholdRegisterdata(val opphold: List<Institusjonsopphold>) : InformasjonskravRegisterdata

    override fun klargjør(kontekst: FlytKontekstMedPerioder): Input {
        return Input(sakService.hentSakFor(kontekst.behandlingId))
    }

    override fun hentData(input: Input): InstitusjonsoppholdRegisterdata {
        val institusjonsopphold = hentInstitusjonsopphold(input.sak)

        return InstitusjonsoppholdRegisterdata(institusjonsopphold)
    }

    override fun oppdater(
        input: Input,
        registerdata: InstitusjonsoppholdRegisterdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val eksisterendeGrunnlag = hentHvisEksisterer(kontekst.behandlingId)
        val institusjonsopphold = registerdata.opphold
        logger.info("Har hentet instopphold " + institusjonsopphold.size)
        // TODO: lagre kun hvis forskjell!
        institusjonsoppholdRepository.lagreOpphold(kontekst.behandlingId, institusjonsopphold)
        logger.info("Har lagret instopphold " + erEndret(eksisterendeGrunnlag, institusjonsopphold))
        return if (erEndret(eksisterendeGrunnlag, institusjonsopphold)) ENDRET else IKKE_ENDRET
    }

    private fun hentInstitusjonsopphold(sak: Sak): List<Institusjonsopphold> {
        return institusjonsoppholdRegisterGateway
            .innhent(sak.person)
            .filter {
                try {
                    it.periode().overlapper(sak.rettighetsperiode)
                } catch (e: IllegalArgumentException) {
                    logger.error(
                        "Ugyldig periode for institusjonsopphold funnet i sak ${sak.id} og ignoreres (startdato=${it.startdato}, sluttdato=${it.sluttdato}",
                        e
                    )
                    false
                }
            }
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag? {
        return institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)
    }

    override fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val sak = sakService.hentSakFor(behandlingId)
        val institusjonsopphold = hentInstitusjonsopphold(sak)

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
        ): InstitusjonsoppholdInformasjonskrav {
            return InstitusjonsoppholdInformasjonskrav(
                SakService(repositoryProvider),
                repositoryProvider.provide(),
                gatewayProvider.provide(),
                TidligereVurderingerImpl(repositoryProvider),
                gatewayProvider.provide<UnleashGateway>()
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