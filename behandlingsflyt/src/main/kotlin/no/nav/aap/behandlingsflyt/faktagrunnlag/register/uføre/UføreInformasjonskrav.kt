package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

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
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdagForBehandling
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreInformasjonskrav.UføreRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Year

class UføreInformasjonskrav(
    private val sakService: SakService,
    private val uføreRepository: UføreRepository,
    private val behandlingRepository: BehandlingRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val uføreRegisterGateway: UføreRegisterGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav<UføreInformasjonskrav.UføreInput, UføreRegisterdata>, KanTriggeRevurdering {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider, gatewayProvider),
        uføreRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
        personopplysningRepository = repositoryProvider.provide(),
        uføreRegisterGateway = gatewayProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
    )

    override val navn = Companion.navn
    private val log = LoggerFactory.getLogger(javaClass)

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        val forrigeInput = oppdatert?.forrigeInput<UføreInput>()
        val nyInput = klargjør(kontekst)

        val forrigeFraDato = forrigeInput?.let {
            utledFraDato(
                forrigeInput.beregningVurdering,
                forrigeInput.sak.rettighetsperiode.fom,
                forrigeInput.fødselsdato
            )
        }
        val nyFraDato = utledFraDato(nyInput.beregningVurdering, nyInput.sak.rettighetsperiode.fom, nyInput.fødselsdato)

        val inputHarEndretSeg = forrigeFraDato != nyFraDato

        return (kontekst.erFørstegangsbehandlingEllerRevurdering() || kontekst.vurderingType == VurderingType.OVERGANG_UFORE_STANS)
                && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
                && (oppdatert.ikkeKjørtSisteKalenderdagForBehandling(kontekst.behandlingId) || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode || inputHarEndretSeg || kontekst.erVurderingsbehovEndretEtterOppdatertInformasjonskrav(
            oppdatert
        ))
    }

    data class UføreInput(
        val sak: Sak,
        val behandlingId: BehandlingId,
        val beregningVurdering: BeregningGrunnlag?,
        val fødselsdato: Fødselsdato?,
    ) : InformasjonskravInput

    data class UføreRegisterdata(val innhentMedHistorikk: Set<Uføre>) : InformasjonskravRegisterdata

    override fun klargjør(kontekst: FlytKontekstMedPerioder): UføreInput {
        val behandlingId = kontekst.behandlingId
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        val fødselsdato =
            personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(kontekst.behandlingId)?.fødselsdato
        return UføreInput(
            sak = sakService.hentSakFor(behandlingId),
            behandlingId = behandlingId,
            beregningVurdering = beregningVurdering,
            fødselsdato = fødselsdato
        )
    }

    override fun hentData(input: UføreInput): UføreRegisterdata {
        return UføreRegisterdata(hentUføregrader(input))
    }

    override fun oppdater(
        input: UføreInput,
        registerdata: UføreRegisterdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        log.info("Oppdaterer uførehistorikk for behandlingen")
        val behandlingId = kontekst.behandlingId
        val uføregrader = registerdata.innhentMedHistorikk

        val eksisterendeGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId)

        if (harEndringerUføre(eksisterendeGrunnlag, uføregrader)) {
            log.info("Fant endringer i uførehistorikk for behandlingen")
            uføreRepository.lagre(behandlingId, uføregrader)
            return ENDRET
        }

        return IKKE_ENDRET
    }

    override fun flettOpplysningerFraAtomærBehandling(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val forrigeBehandlingId = kontekst.forrigeBehandlingId ?: return IKKE_ENDRET
        val forrigeBehandling = behandlingRepository.hent(forrigeBehandlingId)

        /**
         * Skal kun flette inn uføreopplysninger fra atomære behandlinger opprettet som følge av et uførevedtak.
         * Aktivitetspliktbehandlinger, meldekort osv skal ikke påvirke uføreopplysninger i den åpne behandlingen
         * da disse kan bygge på utdatert informasjon
         */
        if (!forrigeBehandling.vurderingsbehov().any { it.type == Vurderingsbehov.OVERGANG_UFORE_AUTOMATISK_STANS }) {
            return IKKE_ENDRET
        }

        val grunnlag = uføreRepository.hentHvisEksisterer(kontekst.behandlingId)
        val forrigeGrunnlag = uføreRepository.hentHvisEksisterer(forrigeBehandlingId)

        val uføregraderForrigeBehandling = forrigeGrunnlag?.vedtak ?: return IKKE_ENDRET
        val mergedUføregrader = grunnlag?.vedtak.orEmpty() + uføregraderForrigeBehandling

        if (mergedUføregrader != grunnlag?.vedtak) {
            uføreRepository.lagre(kontekst.behandlingId, mergedUføregrader)
            return ENDRET
        } else {
            return IKKE_ENDRET
        }
    }

    private fun hentUføregrader(uføreInput: UføreInput): Set<Uføre> {
        val sak = uføreInput.sak
        val beregningVurdering = uføreInput.beregningVurdering
        // prøver å sette fraDato riktig hvis den finnes
        val fraDato = utledFraDato(beregningVurdering, sak.rettighetsperiode.fom, uføreInput.fødselsdato)
        return uføreRegisterGateway.innhentMedHistorikk(sak.person, fraDato)
    }

    private fun utledFraDato(
        beregningVurdering: BeregningGrunnlag?,
        kravdato: LocalDate,
        fødselsdato: Fødselsdato?
    ): LocalDate {
        // Vi henter fra fødselsdato enn så lenge, inntil Team Uføre har laget et endepunkt
        // som kan gi oss uføregrader over tid
        // Se Slack: https://nav-it.slack.com/archives/C06NKNY1399/p1781519190507349
        return fødselsdato?.dato ?: treÅrFør(
            beregningVurdering?.tidspunktVurdering?.ytterligereNedsattArbeidsevneDato
                ?: beregningVurdering?.tidspunktVurdering?.nedsattArbeidsevneEllerStudieevneDato
                ?: kravdato
        )
    }

    private fun treÅrFør(fraOgMed: LocalDate): LocalDate {
        return Year.from(fraOgMed).minusYears(3).atDay(1)
    }

    override fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        val fødselsdato = personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(behandlingId)?.fødselsdato
        val uføregrader =
            hentUføregrader(
                UføreInput(
                    sakService.hentSakFor(behandlingId),
                    behandlingId,
                    beregningVurdering,
                    fødselsdato
                )
            )
        val eksisterendeGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId)

        // Ønsker ikke trigge revurdering automatisk i dette tilfellet enn så lenge
        val gikkFraNullTilTomtGrunnlag = uføregrader.isEmpty() && eksisterendeGrunnlag == null

        return if (harEndringerUføre(eksisterendeGrunnlag, uføregrader) && !gikkFraNullTilTomtGrunnlag) {
            listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_SAMORDNING_UFØRE))
        } else {
            emptyList()
        }
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.UFØRE

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): UføreInformasjonskrav {
            return UføreInformasjonskrav(repositoryProvider, gatewayProvider)
        }

        fun harEndringerUføre(
            eksisterende: UføreGrunnlag?,
            uføregrader: Set<Uføre>
        ): Boolean {
            return if (eksisterende == null) {
                uføregrader.isNotEmpty()
            } else {
                uføregrader != eksisterende.vedtak
            }
        }
    }
}
