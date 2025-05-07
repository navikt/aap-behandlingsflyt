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
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.adapter.ARBEIDSFORHOLDSTATUSER
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.adapter.ArbeidsforholdRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektkomponentenGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapRepositoryImpl
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Duration
import java.time.YearMonth

class LovvalgService private constructor(
    private val sakService: SakService,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val medlemskapRepositoryImpl: MedlemskapRepositoryImpl,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav {
    override val navn = Companion.navn

    private val medlemskapGateway = GatewayProvider.provide<MedlemskapGateway>()

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                tidligereVurderinger.harBehandlingsgrunnlag(kontekst, steg)
    }


    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)

        val medlemskapPerioder =
            medlemskapGateway.innhent(sak.person, Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.fom))
        val arbeidGrunnlag = innhentAARegisterGrunnlag(sak)
        val inntektGrunnlag = innhentAInntektGrunnlag(sak)

        val eksisterendeData = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)
        lagre(kontekst.behandlingId, medlemskapPerioder, arbeidGrunnlag, inntektGrunnlag)
        val nyeData = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)

        return if (nyeData == eksisterendeData) IKKE_ENDRET else ENDRET
    }

    private fun innhentAARegisterGrunnlag(sak: Sak): List<ArbeidINorgeGrunnlag> {
        val request = ArbeidsforholdRequest(
            arbeidstakerId = sak.person.aktivIdent().identifikator,
            arbeidsforholdstatuser = listOf(ARBEIDSFORHOLDSTATUSER.AKTIV.toString())
        )
        return GatewayProvider.provide<ArbeidsforholdGateway>().hentAARegisterData(request)
    }

    private fun innhentAInntektGrunnlag(sak: Sak): List<ArbeidsInntektMaaned> {
        val inntektskomponentGateway = InntektkomponentenGateway()
        return inntektskomponentGateway.hentAInntekt(
            sak.person.aktivIdent().identifikator,
            YearMonth.from(sak.rettighetsperiode.fom.minusMonths(1)),
            YearMonth.from(sak.rettighetsperiode.fom)
        ).arbeidsInntektMaaned
    }

    private fun lagre(
        behandlingId: BehandlingId,
        medlemskapGrunnlag: List<MedlemskapDataIntern>,
        arbeidGrunnlag: List<ArbeidINorgeGrunnlag>,
        inntektGrunnlag: List<ArbeidsInntektMaaned>
    ) {
        val medlId = if (medlemskapGrunnlag.isNotEmpty()) medlemskapRepositoryImpl.lagreUnntakMedlemskap(
            behandlingId,
            medlemskapGrunnlag
        ) else null
        medlemskapArbeidInntektRepository.lagreArbeidsforholdOgInntektINorge(
            behandlingId,
            arbeidGrunnlag,
            inntektGrunnlag,
            medlId
        )
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.LOVVALG

        override fun konstruer(repositoryProvider: RepositoryProvider): LovvalgService {
            val medlemskapArbeidInntektRepository = repositoryProvider.provide<MedlemskapArbeidInntektRepository>()
            val sakRepository = repositoryProvider.provide<SakRepository>()
            return LovvalgService(
                SakService(sakRepository),
                medlemskapArbeidInntektRepository,
                repositoryProvider.provide<MedlemskapRepository>(),
                TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}