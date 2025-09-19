package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektkomponentenGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.EnhetsregisteretGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.Organisasjonsnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.utils.withMdc
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.YearMonth
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class ForutgåendeMedlemskapInformasjonskrav private constructor(
    private val sakService: SakService,
    private val medlemskapForutgåendeRepository: MedlemskapForutgåendeRepository,
    private val grunnlagRepository: MedlemskapArbeidInntektForutgåendeRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val medlemskapGateway: MedlemskapGateway,
    private val arbeidsforholdGateway: ArbeidsforholdGateway,
    private val inntektkomponentenGateway: InntektkomponentenGateway,
    private val enhetsregisteretGateway: EnhetsregisteretGateway
) : Informasjonskrav {

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering()
                && (oppdatert.ikkeKjørtSisteKalenderdag()
                || kontekst.vurderingsbehovRelevanteForSteg.contains(Vurderingsbehov.VURDER_RETTIGHETSPERIODE))
                && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }


    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)

        val medlemskapPerioderFuture = CompletableFuture.supplyAsync(withMdc{ medlemskapGateway.innhent(
            sak.person,
            Periode(sak.rettighetsperiode.fom.minusYears(5), sak.rettighetsperiode.fom)
        ) }, executor)
        val arbeidGrunnlagFuture = CompletableFuture.supplyAsync(withMdc{ innhentAARegisterGrunnlag5år(sak) }, executor)
        val inntektGrunnlagFuture = CompletableFuture.supplyAsync(withMdc{ innhentAInntektGrunnlag5år(sak) }, executor)

        val medlemskapPerioder = medlemskapPerioderFuture.get()
        val arbeidGrunnlag = arbeidGrunnlagFuture.get()
        val inntektGrunnlag = inntektGrunnlagFuture.get()
        val enhetGrunnlag = innhentEREGGrunnlag(inntektGrunnlag)

        val eksisterendeData = grunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
        lagre(kontekst.behandlingId, medlemskapPerioder, arbeidGrunnlag, inntektGrunnlag, enhetGrunnlag)

        val nyeData = grunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)

        return if (nyeData == eksisterendeData) IKKE_ENDRET else ENDRET
    }

    private fun innhentAARegisterGrunnlag5år(sak: Sak): List<ArbeidINorgeGrunnlag> {
        val request = ArbeidsforholdRequest(
            arbeidstakerId = sak.person.aktivIdent().identifikator,
            historikk = true
        )
        return arbeidsforholdGateway.hentAARegisterData(request)
    }

    private fun innhentAInntektGrunnlag5år(sak: Sak): List<ArbeidsInntektMaaned> {
        return inntektkomponentenGateway.hentAInntekt(
            sak.person.aktivIdent().identifikator,
            YearMonth.from(sak.rettighetsperiode.fom.minusYears(5)),
            YearMonth.from(sak.rettighetsperiode.fom)
        ).arbeidsInntektMaaned
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
            CompletableFuture.supplyAsync(withMdc{
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

    private fun lagre(
        behandlingId: BehandlingId,
        medlemskapGrunnlag: List<MedlemskapDataIntern>,
        arbeidGrunnlag: List<ArbeidINorgeGrunnlag>,
        inntektGrunnlag: List<ArbeidsInntektMaaned>,
        enhetGrunnlag: List<EnhetGrunnlag>
    ) {
        val medlId = if (medlemskapGrunnlag.isNotEmpty()) medlemskapForutgåendeRepository.lagreUnntakMedlemskap(
            behandlingId,
            medlemskapGrunnlag
        ) else null
        grunnlagRepository.lagreArbeidsforholdOgInntektINorge(
            behandlingId,
            arbeidGrunnlag,
            inntektGrunnlag,
            medlId,
            enhetGrunnlag
        )
    }

    companion object : Informasjonskravkonstruktør {
        private val executor = Executors.newVirtualThreadPerTaskExecutor()

        override val navn = InformasjonskravNavn.FORUTGÅENDE_MEDLEMSKAP

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): ForutgåendeMedlemskapInformasjonskrav {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val grunnlagRepository = repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
            return ForutgåendeMedlemskapInformasjonskrav(
                SakService(sakRepository),
                repositoryProvider.provide(),
                grunnlagRepository,
                TidligereVurderingerImpl(repositoryProvider),
                gatewayProvider.provide(),
                gatewayProvider.provide(),
                gatewayProvider.provide(),
                gatewayProvider.provide()
            )
        }
    }
}
