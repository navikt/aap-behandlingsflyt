package no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
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

class IverksettBruddSteg private constructor(
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val prosesserBehandlingService: ProsesserBehandlingService
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val effektueringsbehandling = when (kontekst.behandlingType) {
            TypeBehandling.Aktivitetsplikt -> {
                sakOgBehandlingService.finnEllerOpprettBehandling(
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
                sakOgBehandlingService.finnEllerOpprettBehandling(
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
                SakOgBehandlingService(repositoryProvider, gatewayProvider),
                ProsesserBehandlingService(repositoryProvider, gatewayProvider)
            )
        }

        override fun type(): StegType {
            return StegType.IVERKSETT_BRUDD
        }
    }

}