package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class HåndterUførevedtakService(
    private val behandlingService: BehandlingService,
    private val trukketSøknadService: TrukketSøknadService,
    private val rettighetstypeService: RettighetstypeService,
    private val mottaDokumentService: MottaDokumentService,
    private val prosesserBehandling: ProsesserBehandlingService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        rettighetstypeService = RettighetstypeService(repositoryProvider, gatewayProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider),
        prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
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
        if (uførevedtak.resultat.erOpphørEllerEndring()) {
            log.info("Uførevedtak for sak $sakId er opphør eller endring, gjør ingenting i Kelvin med dette")
        } else if (trukketSøknadService.søknadErTrukket(sisteYtelsesBehandling.id)) {
            log.info("Søknad er trukket for sak $sakId, oppretter ikke revurdering ved mottak av uførevedtak")
        } else {
            log.info("Oppretter vurderingsbehov for mottatt uførevedtak for sak $sakId")
            val rettighetstypeTidslinje =
                rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(sisteYtelsesBehandling.id)
            val harRettPåAapEllerEnÅpenBehandling = rettighetstypeTidslinje.isNotEmpty() || sisteYtelsesBehandling.status().erÅpen()
            if (harRettPåAapEllerEnÅpenBehandling) {
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
                prosesserBehandling.triggProsesserBehandling(
                    sakId,
                    behandling.id,
                    vurderingsbehov = listOf(vurderingsbehov),
                )
            }
        }
        mottaDokumentService.markerSomBehandlet(sakId, sisteYtelsesBehandling.id, referanse)
    }
}


