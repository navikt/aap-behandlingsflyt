package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ARBEIDSFORHOLDSTATUSER
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektkomponentenGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.EnhetsregisteretGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.Organisasjonsnummer
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.utils.withMdc
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors


class LovvalgInformasjonskrav private constructor(
    private val sakService: SakService,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val medlemskapRepository: MedlemskapRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val medlemskapGateway: MedlemskapGateway,
    private val arbeidsForholdGateway: ArbeidsforholdGateway,
    private val enhetsregisteretGateway: EnhetsregisteretGateway,
    private val inntektskomponentenGateway: InntektkomponentenGateway
) : Informasjonskrav<LovvalgInformasjonskrav.LovvalgInput, LovvalgInformasjonskrav.LovvalgRegisterData> {
    private val log = LoggerFactory.getLogger(javaClass)

    data class LovvalgInput(
        val sak: Sak,
        val eksisterendeData: MedlemskapArbeidInntektGrunnlag?
    ) : InformasjonskravInput

    data class LovvalgRegisterData(
        val medlemskapPerioder: List<MedlemskapDataIntern>,
        val arbeidGrunnlag: List<ArbeidINorgeGrunnlag>,
        val inntektGrunnlag: List<ArbeidsInntektMaaned>,
        val enhetGrunnlag: List<EnhetGrunnlag>
    ) : InformasjonskravRegisterdata

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

    override fun klargjør(kontekst: FlytKontekstMedPerioder): LovvalgInput {
        val sak = sakService.hent(kontekst.sakId)
        val eksisterendeData = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)
        return LovvalgInput(
            sak = sak,
            eksisterendeData = eksisterendeData
        )
    }

    override fun hentData(input: LovvalgInput): LovvalgRegisterData {
        val sak = input.sak
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
        // TODO: kun lagre hvis forskjell!
        lagre(
            behandlingId = kontekst.behandlingId,
            medlemskapGrunnlag = medlemskapPerioder,
            arbeidGrunnlag = arbeidGrunnlag,
            inntektGrunnlag = inntektGrunnlag,
            enhetGrunnlag = enhetGrunnlag
        )

        val nyData = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)

        return if (nyData == eksisterendeData) IKKE_ENDRET else ENDRET
    }

    private fun innhentAARegisterGrunnlag(sak: Sak): List<ArbeidINorgeGrunnlag> {
        val request = ArbeidsforholdRequest(
            arbeidstakerId = sak.person.aktivIdent().identifikator,
            arbeidsforholdstatuser = listOf(ARBEIDSFORHOLDSTATUSER.AKTIV.toString())
        )
        return arbeidsForholdGateway.hentAARegisterData(request)
    }

    private fun innhentEREGGrunnlag(inntektGrunnlag: List<ArbeidsInntektMaaned>): List<EnhetGrunnlag> {
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
            }, executor)
        }
        return futures.mapNotNull { it.get() }
    }

    private fun innhentAInntektGrunnlag(sak: Sak): List<ArbeidsInntektMaaned> {
        return inntektskomponentenGateway.hentAInntekt(
            sak.person.aktivIdent().identifikator,
            YearMonth.from(sak.rettighetsperiode.fom.minusMonths(1)),
            YearMonth.from(sak.rettighetsperiode.fom)
        ).arbeidsInntektMaaned
    }

    private fun lagre(
        behandlingId: BehandlingId,
        medlemskapGrunnlag: List<MedlemskapDataIntern>,
        arbeidGrunnlag: List<ArbeidINorgeGrunnlag>,
        inntektGrunnlag: List<ArbeidsInntektMaaned>,
        enhetGrunnlag: List<EnhetGrunnlag>,
    ) {
        val perioder = medlemskapGrunnlag.map { Pair(it.fraOgMed, it.tilOgMed) }

        log.info("Lagrer medlemskap, arbeidsforhold og inntekt for behandling $behandlingId. Perioder: $perioder")
        val medlId = if (medlemskapGrunnlag.isNotEmpty()) medlemskapRepository.lagreUnntakMedlemskap(
            behandlingId,
            medlemskapGrunnlag
        ) else null

        medlemskapArbeidInntektRepository.lagreArbeidsforholdOgInntektINorge(
            behandlingId,
            arbeidGrunnlag,
            inntektGrunnlag,
            medlId,
            enhetGrunnlag
        )
    }

    companion object :
        Informasjonskravkonstruktør {
        private val executor = Executors.newVirtualThreadPerTaskExecutor()

        override val navn = InformasjonskravNavn.LOVVALG

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): LovvalgInformasjonskrav {
            val medlemskapArbeidInntektRepository = repositoryProvider.provide<MedlemskapArbeidInntektRepository>()
            val sakRepository = repositoryProvider.provide<SakRepository>()
            return LovvalgInformasjonskrav(
                SakService(sakRepository),
                medlemskapArbeidInntektRepository,
                repositoryProvider.provide<MedlemskapRepository>(),
                TidligereVurderingerImpl(repositoryProvider),
                gatewayProvider.provide(),
                gatewayProvider.provide(),
                gatewayProvider.provide(),
                gatewayProvider.provide(),
            )
        }
    }
}