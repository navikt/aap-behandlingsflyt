package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.UføreSøknadVedtakResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.prosessering.OppdagEndretInformasjonskravJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Prioritet
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class HåndterUførevedtakService(
    private val behandlingService: BehandlingService,
    private val trukketSøknadService: TrukketSøknadService,
    private val rettighetstypeService: RettighetstypeService,
    private val mottaDokumentService: MottaDokumentService,
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val flytJobbRepository: FlytJobbRepository,
    private val unleashGateway: UnleashGateway,
    private val overgangUføreRepository: OvergangUføreRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        rettighetstypeService = RettighetstypeService(repositoryProvider, gatewayProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider),
        prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
        flytJobbRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
        overgangUføreRepository = repositoryProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun håndterMottattUførevedtakHendelse(
        sakId: SakId,
        referanse: InnsendingReferanse,
        uførevedtak: UførevedtakV0,
        mottattTidspunkt: LocalDateTime
    ) {
        val sisteYtelsesBehandling = behandlingService.finnSisteGjeldendeEllerÅpneYtelsesbehandling(sakId)
            ?: error("Finnes ingen ytelsesbehandling for sakId $sakId")
        var behandlingSomHarHåndtertDokument = sisteYtelsesBehandling.id
        if (trukketSøknadService.søknadErTrukket(sisteYtelsesBehandling.id)) {
            log.info("Søknad er trukket for sak $sakId, ignorerer nytt uførevedtak")
        } else if (uførevedtak.resultat.erOpphørEllerEndring()) {
            log.info("Uførevedtak for sak $sakId er opphør eller endring, sjekker om informasjonskrav har endret seg")
            flytJobbRepository.leggTil(
                JobbInput(jobb = OppdagEndretInformasjonskravJobbUtfører).forSak(sakId.toLong()).medCallId().medPrioritet(Prioritet.LAV)
            )
        } else {
            val aktuellPeriode = Periode(uførevedtak.virkningsdato, Tid.MAKS)
            val rettighetstypeTidslinje =
                rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(sisteYtelsesBehandling.id)
                    .begrensetTil(aktuellPeriode)
            val kanHaRettPåAapEtterVirkningsdato =
                rettighetstypeTidslinje.isNotEmpty() || sisteYtelsesBehandling.status().erÅpen()
            val opphørsdato = uførevedtak.opphørsdato(mottattTidspunkt.toLocalDate())

            if (opphørErAlleredeVurdert(uførevedtak, opphørsdato, sisteYtelsesBehandling)) {
                log.info("Gjeldende vurderinger dekker allerede uførevedtaket for sak $sakId, oppretter ikke ny behandling")
            } else if (skalOppretteAutomatiskOpphør11_18(
                    uførevedtak, rettighetstypeTidslinje, opphørsdato, mottattTidspunkt.toLocalDate()
                )) {
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
        uførevedtak: UførevedtakV0,
        rettighetstypeTidslinje: Tidslinje<RettighetsType>,
        opphørsdato: LocalDate,
        mottattDato: LocalDate,
    ): Boolean {
        return rettighetstypeTidslinje.segment(opphørsdato)?.verdi == RettighetsType.VURDERES_FOR_UFØRETRYGD
                && uførevedtak.resultat == UførevedtakResultat.INNV
                && (uførevedtak.virkningsdato.isAfter(mottattDato) ||
                unleashGateway.isEnabled(BehandlingsflytFeature.AutomatiskOpphorUforeTilbakevirkende))
    }

    private fun opphørErAlleredeVurdert(
        uførevedtak: UførevedtakV0,
        opphørsdato: LocalDate,
        behandling: Behandling,
    ): Boolean {
        if (uførevedtak.resultat != UførevedtakResultat.INNV) return false

        val vedtatteVurderinger = behandling.forrigeBehandlingId
            ?.let { overgangUføreRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
        val aktiveVurderinger = overgangUføreRepository.hentHvisEksisterer(behandling.id)?.vurderinger.orEmpty()
        val vurderinger = OvergangUføreGrunnlag((vedtatteVurderinger + aktiveVurderinger).distinct())
            .somOvergangUforevurderingstidslinje()
            .filter {
                val vurdering = it.verdi
                vurdering.fom >= uførevedtak.virkningsdato &&
                        vurdering.brukerRettPåAAP == false &&
                        vurdering.brukerHarFåttVedtakOmUføretrygd in setOf(
                    UføreSøknadVedtakResultat.JA_INNVILGET_FULL,
                    UføreSøknadVedtakResultat.JA_INNVILGET_GRADERT,
                )
            }
            .begrensetTil(Periode(opphørsdato, Tid.MAKS))
            .map { true }
            .komprimer()
        return vurderinger.segment(opphørsdato)?.tom() == Tid.MAKS
    }
}
