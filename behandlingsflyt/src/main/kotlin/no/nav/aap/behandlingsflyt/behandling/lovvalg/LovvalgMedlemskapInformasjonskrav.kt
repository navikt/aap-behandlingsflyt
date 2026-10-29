package no.nav.aap.behandlingsflyt.behandling.lovvalg

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
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.faktagrunnlag.informasjonskravExecutor
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ARBEIDSFORHOLDSTATUSER
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMåned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektkomponentenGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.tilDomene
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.EnhetsregisteretGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.Organisasjonsnummer
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.LovvalgMedlemskapRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.tilDomene
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.behandlingsflyt.utils.withMdc
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.*


class LovvalgMedlemskapInformasjonskrav internal constructor(
    private val sakService: SakService,
    private val lovvalgMedlemskapRepository: LovvalgMedlemskapRepository,
    private val medlemskapUnntakRepository: MedlemskapUnntakRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val medlemskapGateway: MedlemskapGateway,
    private val arbeidsForholdGateway: ArbeidsforholdGateway,
    private val enhetsregisteretGateway: EnhetsregisteretGateway,
    private val inntektskomponentenGateway: InntektkomponentenGateway,
    private val unleashGateway: UnleashGateway,
) : Informasjonskrav<LovvalgMedlemskapInformasjonskrav.LovvalgInput, LovvalgMedlemskapInformasjonskrav.LovvalgRegisterData> {
    private val log = LoggerFactory.getLogger(javaClass)

    data class LovvalgInput(
        val sak: Sak,
        val eksisterendeData: LovvalgMedlemskapGrunnlag?
    ) : InformasjonskravInput

    data class LovvalgRegisterData(
        val medlemskapPerioder: List<MedlemskapDataIntern>,
        val arbeidGrunnlag: List<ArbeidINorgeGrunnlag>,
        val inntektGrunnlag: List<ArbeidsInntektMåned>,
        val enhetGrunnlag: List<EnhetGrunnlag>
    ) : InformasjonskravRegisterdata

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        val fellesRegler = kontekst.erFørstegangsbehandlingEllerRevurdering()
                && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)

        if (fellesRegler && unleashGateway.isEnabled(BehandlingsflytFeature.IkkeSjekkInformasjonskravLovvalgMedlemsskapGrunnlag) && oppdatert != null) {
            return false
        }

        return fellesRegler
                && (oppdatert.ikkeKjørtSisteKalenderdag() || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode)
    }

    override fun klargjør(kontekst: FlytKontekstMedPerioder): LovvalgInput {
        val sak = sakService.hent(kontekst.sakId)
        val eksisterendeData = lovvalgMedlemskapRepository.hentHvisEksisterer(kontekst.behandlingId)
        return LovvalgInput(
            sak = sak,
            eksisterendeData = eksisterendeData
        )
    }

    override fun hentData(input: LovvalgInput): LovvalgRegisterData {
        val sak = input.sak
        val executor = informasjonskravExecutor
        val medlemskapPerioderFuture = CompletableFuture
            .supplyAsync(withMdc { medlemskapGateway.innhent(sak.person, sak.rettighetsperiode) }, executor)
        val arbeidGrunnlagFuture = CompletableFuture
            .supplyAsync(withMdc { innhentAARegisterGrunnlag(sak) }, executor)
        val inntektGrunnlagFuture = CompletableFuture
            .supplyAsync(withMdc { innhentAInntektGrunnlag(sak) }, executor)

        val medlemskapPerioder = medlemskapPerioderFuture.get()
        val arbeidGrunnlag = arbeidGrunnlagFuture.get()
        val inntektGrunnlag = inntektGrunnlagFuture.get()
        val enhetGrunnlag = innhentEREGGrunnlag(inntektGrunnlag)

        return LovvalgRegisterData(
            medlemskapPerioder = medlemskapPerioder,
            arbeidGrunnlag = arbeidGrunnlag,
            inntektGrunnlag = inntektGrunnlag,
            enhetGrunnlag = enhetGrunnlag
        )
    }

    override fun oppdater(
        input: LovvalgInput,
        registerdata: LovvalgRegisterData,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val (medlemskapPerioder, arbeidGrunnlag, inntektGrunnlag, enhetGrunnlag) = registerdata

        val eksisterendeData = input.eksisterendeData

        val nyRegisterdata = LovvalgMedlemskapRegisterGrunnlag(
            inntekterINorgeGrunnlag = inntektGrunnlag.tilDomene(enhetGrunnlag),
            arbeiderINorgeGrunnlag = arbeidGrunnlag,
            medlemskapGrunnlag = medlemskapPerioder
                .takeIf { it.isNotEmpty() }
                ?.let { MedlemskapUnntakGrunnlag(it.tilDomene()) }
        )

        if (nyRegisterdata != eksisterendeData?.registerGrunnlag) {
            lagre(
                behandlingId = kontekst.behandlingId,
                relevantDato = input.sak.rettighetsperiode.fom,
                registerGrunnlag = nyRegisterdata
            )

            return ENDRET
        }

        return IKKE_ENDRET
    }

    private fun innhentAARegisterGrunnlag(sak: Sak): List<ArbeidINorgeGrunnlag> {
        val request = ArbeidsforholdRequest(
            arbeidstakerId = sak.person.aktivIdent().identifikator,
            arbeidsforholdstatuser = listOf(ARBEIDSFORHOLDSTATUSER.AKTIV.toString())
        )
        return arbeidsForholdGateway.hentAARegisterData(request)
    }

    private fun innhentEREGGrunnlag(inntektGrunnlag: List<ArbeidsInntektMåned>): List<EnhetGrunnlag> {
        if (inntektGrunnlag.isEmpty()) return emptyList()

        val orgnumre = inntektGrunnlag.flatMap {
            it.arbeidsInntektInformasjon.inntektListe.map { inntekt ->
                inntekt.virksomhet.identifikator
            }
        }.toSet()

        // EREG har ikke batch-oppslag
        val futures = orgnumre.map { orgnummer ->
            CompletableFuture.supplyAsync(withMdc {
                val response = enhetsregisteretGateway.hentEREGData(Organisasjonsnummer(orgnummer))
                response?.let {
                    EnhetGrunnlag(
                        orgnummer = it.organisasjonsnummer,
                        orgNavn = it.navn.sammensattnavn
                    )
                }
            }, informasjonskravExecutor)
        }
        return futures.mapNotNull { it.get() }
    }

    private fun innhentAInntektGrunnlag(sak: Sak): List<ArbeidsInntektMåned> {
        return inntektskomponentenGateway.hentAInntekt(
            fnr = sak.person.aktivIdent().identifikator,
            fom = YearMonth.from(sak.rettighetsperiode.fom.minusMonths(2)),
            tom = YearMonth.from(sak.rettighetsperiode.fom)
        ).arbeidsInntektMåned
    }

    private fun lagre(
        behandlingId: BehandlingId,
        relevantDato: LocalDate, // TODO: Denne vil etterhvert være på registergrunnlagnivå
        registerGrunnlag: LovvalgMedlemskapRegisterGrunnlag
    ) {
        val medlemskapGrunnlag = registerGrunnlag.medlemskapGrunnlag
        val medlId = if (medlemskapGrunnlag != null) medlemskapUnntakRepository.lagreUnntakMedlemskap(
            behandlingId,
            medlemskapGrunnlag
        ) else null

        lovvalgMedlemskapRepository.lagreRegisterGrunnlag(
            behandlingId,
            relevantDato,
            registerGrunnlag,
            medlId,
        )
    }

    companion object :
        Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.LOVVALG

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): LovvalgMedlemskapInformasjonskrav {
            val lovvalgMedlemskapRepository = repositoryProvider.provide<LovvalgMedlemskapRepository>()
            return LovvalgMedlemskapInformasjonskrav(
                sakService = SakService(repositoryProvider, gatewayProvider),
                lovvalgMedlemskapRepository = lovvalgMedlemskapRepository,
                medlemskapUnntakRepository = repositoryProvider.provide<MedlemskapUnntakRepository>(),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
                medlemskapGateway = gatewayProvider.provide(),
                arbeidsForholdGateway = gatewayProvider.provide(),
                enhetsregisteretGateway = gatewayProvider.provide(),
                inntektskomponentenGateway = gatewayProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
            )
        }
    }
}