package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.AARegisterGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdOversikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektkomponentenGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.YearMonth

class ForutgåendeMedlemskapService private constructor(
    private val sakService: SakService,
    private val medlemskapForutgåendeRepository: MedlemskapForutgåendeRepository,
    private val grunnlagRepository: MedlemskapArbeidInntektForutgåendeRepository
) : Informasjonskrav {
    private val medlemskapGateway = GatewayProvider.provide<MedlemskapGateway>()

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)

        val medlemskapPerioder = medlemskapGateway.innhent(sak.person, Periode(sak.rettighetsperiode.fom.minusYears(5), sak.rettighetsperiode.fom))
        val arbeidGrunnlag = innhentAARegisterGrunnlag5år(sak)
        val inntektGrunnlag = innhentAInntektGrunnlag5år(sak)

        val eksisterendeData = grunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
        lagre(kontekst.behandlingId, medlemskapPerioder, arbeidGrunnlag, inntektGrunnlag)

        val nyeData = grunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)

        return if (nyeData == eksisterendeData) IKKE_ENDRET else ENDRET
    }

    private fun innhentAARegisterGrunnlag5år(sak: Sak): List<ArbeidsforholdOversikt> {
        val request = ArbeidsforholdRequest(
            arbeidstakerId = sak.person.aktivIdent().identifikator,
            historikk = true
        )
        val response = AARegisterGateway().hentAARegisterData(request).arbeidsforholdoversikter
        return response.filter { it.arbeidssted.type.uppercase() == "UNDERENHET" }
    }

    private fun innhentAInntektGrunnlag5år(sak: Sak): List<ArbeidsInntektMaaned> {
        val inntektskomponentGateway = InntektkomponentenGateway()
        return inntektskomponentGateway.hentAInntekt(
            sak.person.aktivIdent().identifikator,
            YearMonth.from(sak.rettighetsperiode.fom.minusYears(5)),
            YearMonth.from(sak.rettighetsperiode.fom)
        ).arbeidsInntektMaaned
    }

    private fun lagre(behandlingId: BehandlingId, medlemskapGrunnlag: List<MedlemskapResponse>, arbeidGrunnlag: List<ArbeidsforholdOversikt>, inntektGrunnlag: List<ArbeidsInntektMaaned>) {
        val medlId = if (medlemskapGrunnlag.isNotEmpty()) medlemskapForutgåendeRepository.lagreUnntakMedlemskap(behandlingId, medlemskapGrunnlag) else null
        grunnlagRepository.lagreArbeidsforholdOgInntektINorge(behandlingId, arbeidGrunnlag, inntektGrunnlag, medlId)
    }

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            if (kontekst.skalBehandlesSomFørstegangsbehandling()) {
                return true
            }
            val relevanteÅrsaker = setOf(ÅrsakTilBehandling.REVURDER_MEDLEMSKAP)
            return kontekst.perioderTilVurdering.flatMap { vurdering -> vurdering.årsaker }
                .any { årsak -> relevanteÅrsaker.contains(årsak) }
        }

        override fun konstruer(connection: DBConnection): ForutgåendeMedlemskapService {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val grunnlagRepository = repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
            return ForutgåendeMedlemskapService(
                SakService(sakRepository),
                MedlemskapForutgåendeRepository(connection),
                grunnlagRepository
            )
        }
    }
}