package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.prosessering.OppdagEndretInformasjonskravJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.orEmpty

class HåndterUførevedtakService(
    private val behandlingService: BehandlingService,
    private val trukketSøknadService: TrukketSøknadService,
    private val rettighetstypeService: RettighetstypeService,
    private val mottaDokumentService: MottaDokumentService,
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val flytJobbRepository: FlytJobbRepository,
    private val unleashGateway: UnleashGateway,
    private val underveisRepository: UnderveisRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        rettighetstypeService = RettighetstypeService(repositoryProvider, gatewayProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider),
        prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
        flytJobbRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
        underveisRepository = repositoryProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun håndterMottattUførevedtakHendelse(
        sakId: SakId,
        referanse: InnsendingReferanse,
        uførevedtak: UførevedtakV0,
        mottattTidspunkt: LocalDateTime
    ) {
        val sisteYtelsesBehandling = behandlingService.finnSisteYtelsesbehandlingFor(sakId)
            ?: error("Finnes ingen ytelsesbehandling for sakId $sakId")
        var behandlingSomHarHåndtertDokument = sisteYtelsesBehandling.id
        if (trukketSøknadService.søknadErTrukket(sisteYtelsesBehandling.id)) {
            log.info("Søknad er trukket for sak $sakId, ignorerer nytt uførevedtak")
        } else if (uførevedtak.resultat.erOpphørEllerEndring()) {
            log.info("Uførevedtak for sak $sakId er opphør eller endring, sjekker om informasjonskrav har endret seg")
            flytJobbRepository.leggTil(
                JobbInput(jobb = OppdagEndretInformasjonskravJobbUtfører).forSak(sakId.toLong()).medCallId()
            )
        } else {
            val aktuellPeriode = Periode(uførevedtak.virkningsdato, Tid.MAKS)
            val rettighetstypeTidslinje =
                rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(sisteYtelsesBehandling.id)
                    .begrensetTil(aktuellPeriode)
            val kanHaRettPåAapEtterVirkningsdato =
                rettighetstypeTidslinje.isNotEmpty() || sisteYtelsesBehandling.status().erÅpen()

            if (skalOppretteAutomatiskOpphør11_18(uførevedtak, rettighetstypeTidslinje, sisteYtelsesBehandling.id)) {
                log.info("Oppretter atomær 11_18 behandling for mottatt uførevedtak for sak $sakId")
                val vurderingsbehov = Vurderingsbehov.OVERGANG_UFORE_AUTOMATISK_STANS
                val opprettetBehandling = behandlingService.finnEllerOpprettBehandling(
                    sakId,
                    VurderingsbehovOgÅrsak(
                        årsak = ÅrsakTilOpprettelse.UFØRE_VEDTAK_HENDELSE,
                        vurderingsbehov = listOf(VurderingsbehovMedPeriode(vurderingsbehov)),
                        opprettet = mottattTidspunkt,
                        beskrivelse = uførevedtak.beskrivelseVurderingsbehov()
                    )
                )
                prosesserBehandlingService.triggProsesserBehandling(
                    opprettetBehandling = opprettetBehandling,
                    vurderingsbehov = listOf(vurderingsbehov),
                )
                val behandlingSomSkalOppdateres = when (opprettetBehandling) {
                    is BehandlingService.MåBehandlesAtomært -> opprettetBehandling.nyBehandling.id
                    is BehandlingService.Ordinær -> opprettetBehandling.åpenBehandling.id
                }
                behandlingSomHarHåndtertDokument = behandlingSomSkalOppdateres
            } else {
                log.info("Oppretter vurderingsbehov for mottatt uførevedtak for sak $sakId")
                if (kanHaRettPåAapEtterVirkningsdato) {
                    val vurderingsbehov = Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                    val behandling = behandlingService.finnEllerOpprettBehandling(
                        sakId,
                        VurderingsbehovOgÅrsak(
                            årsak = ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA,
                            vurderingsbehov = listOf(VurderingsbehovMedPeriode(vurderingsbehov)),
                            opprettet = mottattTidspunkt,
                            beskrivelse = uførevedtak.beskrivelseVurderingsbehov()
                        )
                    ).åpenBehandling ?: error("Klarte ikke å finne eller opprette en behandling for sak $sakId")
                    prosesserBehandlingService.triggProsesserBehandling(
                        sakId,
                        behandling.id,
                        vurderingsbehov = listOf(vurderingsbehov),
                    )
                    behandlingSomHarHåndtertDokument = behandling.id
                } else {
                    log.info("Har ikke åpen behandling eller rett på aap for sakId=$sakId, oppretter ikke revurdering ved uførevedtakhendelse")
                }
            }
        }
        mottaDokumentService.markerSomBehandlet(sakId, behandlingSomHarHåndtertDokument, referanse)
    }

    private fun skalOppretteAutomatiskOpphør11_18(
        uførevedtak: UførevedtakV0, rettighetstypeTidslinje: Tidslinje<RettighetsType>, behandlingId: BehandlingId
    ): Boolean {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.AutomatiskStans1118)) return false // midlertidig early return for logging

        val perioder = underveisRepository.hentHvisEksisterer(behandlingId)?.perioder.orEmpty()
        val oppfylteRettighetsperioder = perioder.any { it.rettighetsType == RettighetsType.VURDERES_FOR_UFØRETRYGD }
        val oppfylteRettighetstypeTidslinje = rettighetstypeTidslinje.filter { it.verdi.hjemmel == RettighetsType.VURDERES_FOR_UFØRETRYGD.hjemmel }
            .isNotEmpty()

        val innvilgetEtter11_18 = oppfylteRettighetsperioder || oppfylteRettighetstypeTidslinje

        log.info("oppfylteRettighetsperioder: $oppfylteRettighetsperioder")
        log.info(perioder.toString())
        log.info("oppfylteRettighetstypeTidslinje: $oppfylteRettighetstypeTidslinje")
        log.info(rettighetstypeTidslinje.toString())

        return unleashGateway.isEnabled(BehandlingsflytFeature.AutomatiskStans1118)
                && innvilgetEtter11_18
                && uførevedtak.resultat == UførevedtakResultat.INNV
                && uførevedtak.virkningsdato.isAfter(LocalDate.now()) // Må endres i senere tid for del 2
    }
}
