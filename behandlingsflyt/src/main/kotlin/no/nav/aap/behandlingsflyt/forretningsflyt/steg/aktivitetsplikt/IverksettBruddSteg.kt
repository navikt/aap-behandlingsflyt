package no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class IverksettBruddSteg private constructor(
    private val behandlingService: BehandlingService,
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val avbrytAktivitetspliktbehandlingService: AvbrytAktivitetspliktbehandlingService
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (avbrytAktivitetspliktbehandlingService.behandlingErAvbrutt(kontekst.behandlingId)) {
            log.info("Behandling ${kontekst.behandlingId} er avbrutt, hopper over iverksettelse av brudd")
            return Fullført
        }
        val effektueringsbehandling = when (kontekst.behandlingType) {
            TypeBehandling.Aktivitetsplikt -> {
                behandlingService.finnEllerOpprettBehandling(
                    kontekst.sakId,
                    VurderingsbehovOgÅrsak(
                        årsak = ÅrsakTilOpprettelse.AKTIVITETSPLIKT, vurderingsbehov = listOf(
                            VurderingsbehovMedPeriode(
                                Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT
                            )
                        )
                    )
                )
            }

            TypeBehandling.Aktivitetsplikt11_9 -> {
                behandlingService.finnEllerOpprettBehandling(
                    kontekst.sakId,
                    VurderingsbehovOgÅrsak(
                        årsak = ÅrsakTilOpprettelse.AKTIVITETSPLIKT_11_9, vurderingsbehov = listOf(
                            VurderingsbehovMedPeriode(
                                Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT_11_9
                            )
                        )
                    )
                )
            }

            else -> error("Behandlingstype ikke støttet")
        }
        prosesserBehandlingService.triggProsesserBehandling(effektueringsbehandling)

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return IverksettBruddSteg(
                BehandlingService(repositoryProvider, gatewayProvider),
                ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                AvbrytAktivitetspliktbehandlingService(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.IVERKSETT_BRUDD
        }
    }

}