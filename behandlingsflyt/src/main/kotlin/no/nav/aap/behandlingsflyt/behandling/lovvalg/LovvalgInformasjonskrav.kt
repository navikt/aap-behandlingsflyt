package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
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
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Duration
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
) : Informasjonskrav {
    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering()
                && (oppdatert.ikkeKjørtSiste(Duration.ofHours(1))
                || kontekst.vurderingsbehovRelevanteForSteg.contains(Vurderingsbehov.VURDER_RETTIGHETSPERIODE))
                && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)

        val medlemskapPerioder =
            medlemskapGateway.innhent(sak.person, sak.rettighetsperiode)
        val arbeidGrunnlag = innhentAARegisterGrunnlag(sak)
        val inntektGrunnlag = innhentAInntektGrunnlag(sak)
        val enhetGrunnlag = innhentEREGGrunnlag(inntektGrunnlag)

        val eksisterendeData = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)
        lagre(kontekst.behandlingId, medlemskapPerioder, arbeidGrunnlag, inntektGrunnlag, enhetGrunnlag)
        val nyeData = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)

        return if (nyeData == eksisterendeData) IKKE_ENDRET else ENDRET
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
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        val futures = orgnumre.map { orgnummer ->
            CompletableFuture.supplyAsync({
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

    companion object : Informasjonskravkonstruktør {
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