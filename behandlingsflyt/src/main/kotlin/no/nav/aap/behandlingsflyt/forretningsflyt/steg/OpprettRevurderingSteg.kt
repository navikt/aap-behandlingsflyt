package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class OpprettRevurderingSteg(
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val samordningYtelseVurderingRepository: SamordningVurderingRepository,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val trukketSøknadService: TrukketSøknadService,
) : BehandlingSteg {
    private val logger = LoggerFactory.getLogger(javaClass)
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (trukketSøknadService.søknadErTrukket(kontekst.behandlingId)) {
                    return Fullført
                }

                val samordningVurdering =
                    samordningYtelseVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)
                        ?: return Fullført

                if (!erUsikkerhetTilknyttetMaksSykepengerDato(samordningVurdering)) return Fullført

                logger.info("Oppretter revurdering. SakID: ${kontekst.sakId}")
                val behandling = sakOgBehandlingService.finnEllerOpprettBehandling(
                    sakId = kontekst.sakId,
                    vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            type = Vurderingsbehov.REVURDER_SAMORDNING,
                        )
                    ),
                    årsakTilOpprettelse = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
                )

                val behandlingSkrivelås =
                    låsRepository.låsBehandling(behandling.id)

                prosesserBehandling.triggProsesserBehandling(behandling.sakId, behandling.id)
                låsRepository.verifiserSkrivelås(behandlingSkrivelås)

                return Fullført
            }

            VurderingType.REVURDERING, VurderingType.MELDEKORT, VurderingType.IKKE_RELEVANT -> {
                Fullført
            }
        }
    }

    private fun erUsikkerhetTilknyttetMaksSykepengerDato(samordningVurdering: SamordningVurderingGrunnlag): Boolean {
        return samordningVurdering.maksDatoEndelig != true && samordningVurdering.fristNyRevurdering != null
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {

            return OpprettRevurderingSteg(
                SakOgBehandlingService(repositoryProvider),
                samordningYtelseVurderingRepository = repositoryProvider.provide(),
                låsRepository = repositoryProvider.provide(),
                prosesserBehandling = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                trukketSøknadService = TrukketSøknadService(repositoryProvider),
            )
        }

        override fun type(): StegType {
            return StegType.OPPRETT_REVURDERING
        }
    }
}