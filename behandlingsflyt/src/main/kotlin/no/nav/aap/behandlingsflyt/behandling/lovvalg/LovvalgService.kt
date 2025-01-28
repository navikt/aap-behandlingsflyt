package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.AARegisterGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ARBEIDSFORHOLDSTATUSER
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdOversikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektkomponentenGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.YearMonth

class LovvalgService private constructor(
    private val medlemskapGateway: MedlemskapGateway,
    private val sakService: SakService,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val medlemskapRepository: MedlemskapRepository
): Informasjonskrav {

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)

        val medlemskapPerioder = medlemskapGateway.innhent(sak.person, Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.fom))
        val arbeidGrunnlag = innhentAARegisterGrunnlag(sak)
        val inntektGrunnlag = innhentAInntektGrunnlag(sak)

        val eksisterendeData = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)
        lagre(kontekst.behandlingId, medlemskapPerioder, arbeidGrunnlag, inntektGrunnlag)
        val nyeData = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)

        return if (nyeData == eksisterendeData) IKKE_ENDRET else ENDRET
    }

    private fun innhentAARegisterGrunnlag(sak: Sak): List<ArbeidsforholdOversikt> {
        val request = ArbeidsforholdRequest(sak.person.aktivIdent().identifikator, listOf(ARBEIDSFORHOLDSTATUSER.AKTIV.toString()))
        return AARegisterGateway().hentAARegisterData(request).arbeidsforholdoversikter.filter { it.arbeidssted.type.uppercase() == "UNDERENHET" }
    }

    private fun innhentAInntektGrunnlag(sak: Sak): List<ArbeidsInntektMaaned> {
        val inntektskomponentGateway = InntektkomponentenGateway()
        return inntektskomponentGateway.hentAInntekt(
            sak.person.aktivIdent().identifikator,
            YearMonth.from(sak.rettighetsperiode.fom),
            YearMonth.from(sak.rettighetsperiode.fom)
        ).arbeidsInntektMaaned
    }

    private fun lagre(behandlingId: BehandlingId, medlemskapGrunnlag: List<MedlemskapResponse>, arbeidGrunnlag: List<ArbeidsforholdOversikt>, inntektGrunnlag: List<ArbeidsInntektMaaned>) {
        val medlId = if (medlemskapGrunnlag.isNotEmpty()) medlemskapRepository.lagreUnntakMedlemskap(behandlingId, medlemskapGrunnlag) else null
        medlemskapArbeidInntektRepository.lagreArbeidsforholdOgInntektINorge(behandlingId, arbeidGrunnlag, inntektGrunnlag, medlId)
    }

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            if (kontekst.skalBehandlesSomFørstegangsbehandling()) {
                return true
            }
            val relevanteÅrsaker = setOf(ÅrsakTilBehandling.ENDRING_MEDLEMSKAP)
            return kontekst.perioderTilVurdering.flatMap { vurdering -> vurdering.årsaker }
                .any { årsak -> relevanteÅrsaker.contains(årsak) }
        }

        override fun konstruer(connection: DBConnection): LovvalgService {
            val repositoryProvider = RepositoryProvider(connection)
            val medlemskapArbeidInntektRepository = repositoryProvider.provide<MedlemskapArbeidInntektRepository>()
            val sakRepository = repositoryProvider.provide<SakRepository>()
            return LovvalgService(
                MedlemskapGateway(),
                SakService(sakRepository),
                medlemskapArbeidInntektRepository,
                MedlemskapRepository(connection)
            )
        }
    }
}