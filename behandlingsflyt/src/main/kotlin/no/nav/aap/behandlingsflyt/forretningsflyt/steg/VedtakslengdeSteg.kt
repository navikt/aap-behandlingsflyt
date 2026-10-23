package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.arena.ArenaMigreringMapper
import no.nav.aap.behandlingsflyt.arena.ArenaMigreringService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeUtvidelse
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.MigrerVurderingFraArena
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VedtakslengdeSteg(
    private val vedtakslengdeService: VedtakslengdeService,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val arenaMigreringService: ArenaMigreringService,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg, MigrerVurderingFraArena {
    private val log = LoggerFactory.getLogger(VedtakslengdeSteg::class.java)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vedtakslengdeService = VedtakslengdeService(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        arenaMigreringService = ArenaMigreringService(repositoryProvider, gatewayProvider),
        unleashGateway = gatewayProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {

                // Lagrer en automatisk vurdering med sluttdato - saksbehandler kan manuelt overstyre denne
                vedtakslengdeService.lagreAutomatiskVedtakslengde(
                    behandlingId = kontekst.behandlingId,
                    forrigeBehandlingId = kontekst.forrigeBehandlingId,
                    rettighetsperiode = kontekst.rettighetsperiode
                )

                avklaringsbehovService.oppdaterAvklaringsbehov(
                    definisjon = Definisjon.AVKLAR_VEDTAKSLENGDE,
                    vedtakBehøverVurdering = {
                        when {
                            tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                            kontekst.vurderingsbehovRelevanteForSteg.isEmpty() -> false
                            else ->
                                Vurderingsbehov.VEDTAKSLENGDE_MANUELT in kontekst.vurderingsbehovRelevanteForSteg
                        }
                    },
                    erTilstrekkeligVurdert = { true },
                    tilbakestillGrunnlag = { }, // Har ingen effekt da avklaringsbehovet er MANUELT_FRIVILLIG
                    kontekst = kontekst
                )
            }

            VurderingType.MIGERING_FRA_ARENA -> {
                val girAvslag = tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())
                val migrererFraArena =
                    unleashGateway.isEnabled(BehandlingsflytFeature.MigrererVedtakslengdeFraArenaAutomatisk)
                if (migrererFraArena && !girAvslag) {
                    migrerVurderingFraArena(kontekst)
                }

                avklaringsbehovService.oppdaterAvklaringsbehov(
                    definisjon = Definisjon.AVKLAR_VEDTAKSLENGDE,
                    vedtakBehøverVurdering = {
                        when {
                            girAvslag -> false
                            // Migrert vedtakslengde trenger ikke manuell vurdering, men kan overstyres av saksbehandler
                            migrererFraArena ->
                                Vurderingsbehov.VEDTAKSLENGDE_MANUELT in kontekst.vurderingsbehovRelevanteForSteg

                            else -> true
                        }
                    },
                    erTilstrekkeligVurdert = {
                        vedtakslengdeService.hentVedtakslengdeGrunnlag(kontekst.behandlingId)?.gjeldendeVurdering() != null
                    },
                    tilbakestillGrunnlag = { }, // Har ingen effekt da avklaringsbehovet er MANUELT_FRIVILLIG
                    kontekst = kontekst
                )
            }

            VurderingType.MIGRER_RETTIGHETSPERIODE -> {
                vedtakslengdeService.lagreGjeldendeSluttdatoHvisIkkeEksisterer(
                    behandlingId = kontekst.behandlingId,
                    forrigeBehandlingId = kontekst.forrigeBehandlingId,
                    rettighetsperiode = kontekst.rettighetsperiode
                )
            }

            VurderingType.UTVID_VEDTAKSLENGDE -> {
                val vedtakslengdeUtvidelse = vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(
                    behandlingId = kontekst.behandlingId,
                    forrigeBehandlingId = kontekst.forrigeBehandlingId,
                )

                if (vedtakslengdeUtvidelse is VedtakslengdeUtvidelse.Automatisk) {
                    vedtakslengdeService.utvidVedtakslengde(
                        behandlingId = kontekst.behandlingId,
                        forrigeBehandlingId = kontekst.forrigeBehandlingId,
                        vedtakslengdeUtvidelse = vedtakslengdeUtvidelse,
                    )
                } else {
                    log.info("Ingen automatisk utvidelse av vedtakslengde: $vedtakslengdeUtvidelse")
                }
            }

            else -> {} // Noop
        }

        return Fullført
    }

    override fun migrerVurderingFraArena(kontekst: FlytKontekstMedPerioder) {
        require(kontekst.erMigreringFraArena()) {
            "Kan ikke migrere vedtakslengde fra Arena for sak ${kontekst.sakId} fordi vurderingstype ikke er migrering"
        }

        val vedtaksperiode = arenaMigreringService.hentVedtaksperiode(kontekst.sakId)
        arenaMigreringService.lagreMigreringsdataForSporing(kontekst.behandlingId, type(), vedtaksperiode)

        val vurdering = ArenaMigreringMapper.mapVedtakslengdeVurdering(
            fraArena = vedtaksperiode,
            behandlingId = kontekst.behandlingId,
        )
        vedtakslengdeService.lagreMigrertVedtakslengde(kontekst.behandlingId, vurdering)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VedtakslengdeSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_VEDTAKSLENGDE
        }
    }
}
