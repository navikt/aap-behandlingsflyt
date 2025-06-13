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
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.adapter.ArbeidsforholdRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektkomponentenGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.EnhetsregisteretGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.adapter.EnhetsregisterOrganisasjonRequest
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Duration
import java.time.YearMonth

class ForutgåendeMedlemskapService private constructor(
    private val sakService: SakService,
    private val medlemskapForutgåendeRepository: MedlemskapForutgåendeRepository,
    private val grunnlagRepository: MedlemskapArbeidInntektForutgåendeRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav {
    private val medlemskapGateway = GatewayProvider.provide<MedlemskapGateway>()

    override val navn = Companion.navn

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering()
            && (oppdatert.ikkeKjørtSiste(Duration.ofHours(1))
                || kontekst.årsakerTilBehandling.contains(ÅrsakTilBehandling.VURDER_RETTIGHETSPERIODE))
            && tidligereVurderinger.harBehandlingsgrunnlag(kontekst, steg)
    }


    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)

        val medlemskapPerioder = medlemskapGateway.innhent(sak.person, Periode(sak.rettighetsperiode.fom.minusYears(5), sak.rettighetsperiode.fom))
        val arbeidGrunnlag = innhentAARegisterGrunnlag5år(sak)
        val inntektGrunnlag = innhentAInntektGrunnlag5år(sak)
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
        return GatewayProvider.provide<ArbeidsforholdGateway>().hentAARegisterData(request)
    }

    private fun innhentAInntektGrunnlag5år(sak: Sak): List<ArbeidsInntektMaaned> {
        val inntektskomponentGateway = InntektkomponentenGateway()
        return inntektskomponentGateway.hentAInntekt(
            sak.person.aktivIdent().identifikator,
            YearMonth.from(sak.rettighetsperiode.fom.minusYears(5)),
            YearMonth.from(sak.rettighetsperiode.fom)
        ).arbeidsInntektMaaned
    }

    private fun innhentEREGGrunnlag(inntektGrunnlag: List<ArbeidsInntektMaaned>): List<EnhetGrunnlag> {
        if (inntektGrunnlag.isEmpty()) return emptyList()

        val orgnumre = inntektGrunnlag.flatMap {
            it.arbeidsInntektInformasjon.inntektListe.map {
                    inntekt -> inntekt.virksomhet.identifikator
            }
        }.toSet()
        val gateway = GatewayProvider.provide<EnhetsregisteretGateway>()

        // EREG har ikke batch-oppslag
        val enhetsGrunnlag = orgnumre.mapNotNull {
            val response = gateway.hentEREGData(EnhetsregisterOrganisasjonRequest(it)) ?: return@mapNotNull null
            EnhetGrunnlag(
                orgnummer = response.organisasjonsnummer,
                orgNavn = response.navn.sammensattnavn
            )
        }
        return enhetsGrunnlag
    }

    private fun lagre(
        behandlingId: BehandlingId,
        medlemskapGrunnlag: List<MedlemskapDataIntern>,
        arbeidGrunnlag: List<ArbeidINorgeGrunnlag>,
        inntektGrunnlag: List<ArbeidsInntektMaaned>,
        enhetGrunnlag: List<EnhetGrunnlag>
    ) {
        val medlId = if (medlemskapGrunnlag.isNotEmpty()) medlemskapForutgåendeRepository.lagreUnntakMedlemskap(behandlingId, medlemskapGrunnlag) else null
        grunnlagRepository.lagreArbeidsforholdOgInntektINorge(behandlingId, arbeidGrunnlag, inntektGrunnlag, medlId, enhetGrunnlag)
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.FORUTGÅENDE_MEDLEMSKAP

        override fun konstruer(repositoryProvider: RepositoryProvider): ForutgåendeMedlemskapService {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val grunnlagRepository = repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
            return ForutgåendeMedlemskapService(
                SakService(sakRepository),
                repositoryProvider.provide(),
                grunnlagRepository,
                TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}