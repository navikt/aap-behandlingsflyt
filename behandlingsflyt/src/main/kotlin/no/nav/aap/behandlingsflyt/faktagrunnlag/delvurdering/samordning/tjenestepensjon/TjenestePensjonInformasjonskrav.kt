package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonInformasjonskrav.TjenestePensjonRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class TjenestePensjonInformasjonskrav(
    private val tjenestePensjonRepository: TjenestePensjonRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val tpGateway: TjenestePensjonGateway,
    private val sakOgBehandlingService: SakOgBehandlingService,
) : Informasjonskrav<TjenestePensjonInformasjonskrav.TjenestePensjonInput, TjenestePensjonRegisterdata>,
    KanTriggeRevurdering {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.SAMORDNING_TJENESTEPENSJON

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): TjenestePensjonInformasjonskrav {
            return TjenestePensjonInformasjonskrav(
                tjenestePensjonRepository = repositoryProvider.provide(),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
                tpGateway = gatewayProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
            )
        }

        fun harEndringerITjenestePensjon(
            eksisterendeData: List<TjenestePensjonForhold>?,
            tjenestePensjon: List<TjenestePensjonForhold>
        ): Boolean {
            return eksisterendeData == null || eksisterendeData.toSet() != tjenestePensjon.toSet()
        }
    }

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

    data class TjenestePensjonInput(
        val personIdent: String,
        val rettighetsperiode: Periode,
        val eksisterendeData: List<TjenestePensjonForhold>?
    ) : InformasjonskravInput

    data class TjenestePensjonRegisterdata(
        val tjenestePensjon: List<TjenestePensjonForhold>
    ) : InformasjonskravRegisterdata

    override fun klargjør(kontekst: FlytKontekstMedPerioder): TjenestePensjonInput {
        val eksisterendeData = tjenestePensjonRepository.hentHvisEksisterer(kontekst.behandlingId)
        val sak = sakOgBehandlingService.hentSakFor(kontekst.behandlingId)
        val personIdent = sak.person.aktivIdent().identifikator
        return TjenestePensjonInput(
            personIdent = personIdent,
            rettighetsperiode = sak.rettighetsperiode,
            eksisterendeData = eksisterendeData
        )
    }

    override fun hentData(input: TjenestePensjonInput): TjenestePensjonRegisterdata {
        val (personIdent, rettigetsperiode) = input
        return TjenestePensjonRegisterdata(
            tpGateway.hentTjenestePensjon(
                personIdent,
                rettigetsperiode
            )
        )
    }


    override fun oppdater(
        input: TjenestePensjonInput,
        registerdata: TjenestePensjonRegisterdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val (tjenestePensjon) = registerdata

        if (harEndringerITjenestePensjon(input.eksisterendeData, tjenestePensjon)) {
            log.info("Oppdaterer tjeneste pensjon for behandling ${kontekst.behandlingId}. Tjeneste pensjon funnet: ${tjenestePensjon.size}")
            tjenestePensjonRepository.lagre(kontekst.behandlingId, tjenestePensjon)
            return Informasjonskrav.Endret.ENDRET
        }

        return Informasjonskrav.Endret.IKKE_ENDRET
    }

    private fun hentTjenestePensjon(behandlingId: BehandlingId): List<TjenestePensjonForhold> {
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        val personIdent = sak.person.aktivIdent().identifikator

        return tpGateway.hentTjenestePensjon(
            personIdent,
            sak.rettighetsperiode
        ).also {
            log.info("Hentet tjenestepensjon for person i sak ${sak.saksnummer}. Antall: ${it.size}")
        }
    }

    override fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        val tjenestePensjon = hentTjenestePensjon(behandlingId)
        val eksisterendeData = tjenestePensjonRepository.hentHvisEksisterer(behandlingId)

        // Ønsker ikke trigge revurdering automatisk i dette tilfellet enn så lenge
        val gikkFraNullTilTomtGrunnlag = tjenestePensjon.isEmpty() && eksisterendeData == null

        return if (!gikkFraNullTilTomtGrunnlag && harEndringerITjenestePensjon(eksisterendeData, tjenestePensjon)) {
            listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON))
        } else {
            emptyList()
        }
    }
}