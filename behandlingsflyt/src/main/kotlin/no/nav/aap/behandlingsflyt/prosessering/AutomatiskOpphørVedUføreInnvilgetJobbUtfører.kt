package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRegisterGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.UføreSøknadVedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year

private const val REFERANSE = "referanse"
private const val MOTTATT_TIDSPUNKT = "mottattTidspunkt"

class AutomatiskOpphørVedUføreInnvilgetJobbUtfører(
    private val sakService: SakService,
    private val behandlingService: BehandlingService,
    private val mottaDokumentService: MottaDokumentService,
    private val overgangUføreRepository: OvergangUføreRepository,
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val uføreRegisterGateway: UføreRegisterGateway,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val referanse = DefaultJsonMapper.fromJson<InnsendingReferanse>(input.parameter(REFERANSE))
        val mottattTidspunkt = DefaultJsonMapper.fromJson<LocalDateTime>(input.parameter(MOTTATT_TIDSPUNKT))
        val uførevedtak = input.payload<UførevedtakV0>()
        val sak = sakService.hent(sakId)
        val vedtakResultat = utledVedtakResultat(sak.person, uførevedtak.virkningsdato)

        val sisteYtelsesBehandling = behandlingService.finnSisteYtelsesbehandlingFor(sakId)
            ?: error("Finnes ingen ytelsesbehandling for sakId $sakId")

        håndterAutomatiskStans118(
            sakId = sakId,
            uførevedtak = uførevedtak,
            mottattTidspunkt = mottattTidspunkt,
            vedtakResultat = vedtakResultat,
        )
        mottaDokumentService.markerSomBehandlet(sakId, sisteYtelsesBehandling.id, referanse)
    }

    private fun håndterAutomatiskStans118(
        sakId: SakId,
        uførevedtak: UførevedtakV0,
        mottattTidspunkt: LocalDateTime,
        vedtakResultat: UføreSøknadVedtakResultat,
    ): BehandlingService.OpprettetBehandling {
        val vurderingsbehov = Vurderingsbehov.OVERGANG_UFORE
        val opprettetBehandling = behandlingService.finnEllerOpprettBehandling(
            sakId,
            VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(vurderingsbehov)),
                opprettet = mottattTidspunkt,
                beskrivelse = uførevedtak.beskrivelseVurderingsbehov()
            )
        )
        val behandling = (opprettetBehandling as? BehandlingService.MåBehandlesAtomært)?.nyBehandling
            ?: error("OVERGANG_UFORE skal opprettes som atomær fasttrack for sakId $sakId")
        val vedtatteVurderinger = behandling.forrigeBehandlingId
            ?.let { overgangUføreRepository.hentHvisEksisterer(it) }
            ?.vurderinger
            .orEmpty()
        val eksisterendeVurderinger = overgangUføreRepository.hentHvisEksisterer(behandling.id)?.vurderinger.orEmpty()
        val harAutomatiskVurderingAllerede = eksisterendeVurderinger.any {
            it.vurdertAv == SYSTEMBRUKER.ident && it.fom == uførevedtak.virkningsdato
        }

        if (!harAutomatiskVurderingAllerede) {
            val vurderingerSomSkalLagres = (eksisterendeVurderinger + vedtatteVurderinger)
            overgangUføreRepository.lagre(
                behandlingId = behandling.id,
                overgangUføreVurderinger = vurderingerSomSkalLagres + OvergangUføreVurdering(
                    begrunnelse = SYSTEMBRUKER.ident,
                    brukerHarSøktOmUføretrygd = true,
                    brukerHarFåttVedtakOmUføretrygd = vedtakResultat,
                    brukerRettPåAAP = false,
                    fom = uførevedtak.virkningsdato,
                    tom = null,
                    vurdertAv = SYSTEMBRUKER.ident,
                    vurdertIBehandling = behandling.id,
                    opprettet = Instant.now(),
                )
            )
        }
        prosesserBehandlingService.triggProsesserBehandling(
            opprettetBehandling = opprettetBehandling,
            vurderingsbehov = listOf(vurderingsbehov),
        )
        return opprettetBehandling
    }

    private fun utledVedtakResultat(
        person: Person,
        virkningsdato: LocalDate,
    ): UføreSøknadVedtakResultat {
        val oppslagsdato = Year.from(virkningsdato).minusYears(3).atDay(1)
        val uføregrad = uføreRegisterGateway.innhentMedHistorikk(person, oppslagsdato)
            .tilTidslinje()
            .segment(virkningsdato)
            ?.verdi
            ?.prosentverdi()

        return when (uføregrad) {
            100 -> UføreSøknadVedtakResultat.JA_INNVILGET_FULL
            null -> UføreSøknadVedtakResultat.JA_INNVILGET_GRADERT
            else -> UføreSøknadVedtakResultat.JA_INNVILGET_GRADERT
        }
    }

    companion object : ProvidersJobbSpesifikasjon {
        fun nyJobb(
            sakId: SakId,
            referanse: InnsendingReferanse,
            uførevedtak: UførevedtakV0,
            mottattTidspunkt: LocalDateTime,
        ) = JobbInput(AutomatiskOpphørVedUføreInnvilgetJobbUtfører).apply {
            forSak(sakId.toLong())
            medCallId()
            medParameter(REFERANSE, DefaultJsonMapper.toJson(referanse))
            medParameter(MOTTATT_TIDSPUNKT, DefaultJsonMapper.toJson(mottattTidspunkt))
            medPayload(uførevedtak)
        }

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return AutomatiskOpphørVedUføreInnvilgetJobbUtfører(
                sakService = SakService(repositoryProvider, gatewayProvider),
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                mottaDokumentService = MottaDokumentService(repositoryProvider),
                overgangUføreRepository = repositoryProvider.provide(),
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                uføreRegisterGateway = gatewayProvider.provide(),
            )
        }

        override val type = "hendelse.automatiskStans118"
        override val navn = "Automatisk stans 11-18"
        override val beskrivelse = "Håndterer automatisk stans 11-18 for innvilget uførevedtak."
    }
}
