package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.somSykdomsvurderingTidslinje
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime
import java.time.ZoneId

class FatteVedtakSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val trekkKlageService: TrekkKlageService,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val avbrytRevurderingService: AvbrytRevurderingService,
    private val trukketSøknadService: TrukketSøknadService,
    private val avbrytAktivitetspliktbehandlingService: AvbrytAktivitetspliktbehandlingService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val vedtakService: VedtakService,
    private val virkningstidspunktService: VirkningstidspunktService,
    private val sykdomRepository: SykdomRepository,
    private val behandlingService: BehandlingService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val skalHoppeOverBeslutter =
            unleashGateway.isEnabled(BehandlingsflytFeature.HoppOverBeslutterVedAvslagSykdom)
                    && behandlingService.utledFaktiskBehandlingstype(kontekst.behandlingId) == TypeBehandling.Førstegangsbehandling
                    && skalHoppeOverBeslutterPåGrunnAvAvslagSykdom(
                kontekst
            )

        val vedtakBehøverVurdering = vedtakBehøverVurdering(kontekst, avklaringsbehovene, skalHoppeOverBeslutter)
        val erTilstrekkeligVurdert = erTilstrekkeligVurdert(kontekst, avklaringsbehovene, skalHoppeOverBeslutter)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.FATTE_VEDTAK,
            vedtakBehøverVurdering = { vedtakBehøverVurdering },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert },
            tilbakestillGrunnlag = {},
            kontekst = kontekst
        )

        val vedtakstidspunkt = if (vedtakBehøverVurdering)
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FATTE_VEDTAK)
                ?.historikk
                ?.filter { it.status == Status.AVSLUTTET }
                ?.maxOrNull()
                ?.tidsstempel
        else
            LocalDateTime.now(ZoneId.of("Europe/Oslo"))

        if (erTilstrekkeligVurdert && vedtakstidspunkt != null && skalLagreYtelsesvedtak(kontekst)) {
            vedtakService.lagreVedtak(
                behandlingId = kontekst.behandlingId,
                vedtakstidspunkt = vedtakstidspunkt,
                virkningstidspunkt = virkningstidspunktService.finnVirkningstidspunkt(kontekst.behandlingId),
            )
        }

        return Fullført
    }

    private fun skalLagreYtelsesvedtak(kontekst: FlytKontekstMedPerioder): Boolean {
        when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling -> {
                return !trukketSøknadService.søknadErTrukket(kontekst.behandlingId)
            }

            TypeBehandling.Revurdering -> {
                return !avbrytRevurderingService.revurderingErAvbrutt(kontekst.behandlingId)
            }

            TypeBehandling.Tilbakekreving,
            TypeBehandling.Klage,
            TypeBehandling.SvarFraAndreinstans,
            TypeBehandling.OppfølgingsBehandling,
            TypeBehandling.Aktivitetsplikt,
            TypeBehandling.Aktivitetsplikt11_9 -> {
                return false
            }
        }
    }

    private fun vedtakBehøverVurdering(
        kontekst: FlytKontekstMedPerioder,
        avklaringsbehovene: Avklaringsbehovene,
        skalHoppeOverBeslutter: Boolean
    ): Boolean {
        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) ||
            trekkKlageService.klageErTrukket(kontekst.behandlingId) ||
            avbrytAktivitetspliktbehandlingService.behandlingErAvbrutt(kontekst.behandlingId) ||
            skalHoppeOverBeslutter
        ) {
            return false
        }

        if (kontekst.behandlingType == TypeBehandling.Klage) {
            val klageresultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
            if (klageresultat is Opprettholdes) {
                return false
            }
        }

        return avklaringsbehovene.harAvklaringsbehovSomKreverToTrinn()
    }

    private fun erTilstrekkeligVurdert(
        kontekst: FlytKontekstMedPerioder,
        avklaringsbehovene: Avklaringsbehovene,
        skalHoppeOverBeslutter: Boolean
    ): Boolean {
        val erTrukketEllerIngenGrunnlag =
            tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) ||
                    trekkKlageService.klageErTrukket(kontekst.behandlingId)

        return when {
            erTrukketEllerIngenGrunnlag -> true
            skalHoppeOverBeslutter -> true
            avklaringsbehovene.harAvklaringsbehovSomKreverToTrinnMenIkkeErGodkjent() -> false
            else -> true
        }
    }

    private fun skalHoppeOverBeslutterPåGrunnAvAvslagSykdom(kontekst: FlytKontekstMedPerioder): Boolean {
        val sykdomsGrunnlag = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId) ?: return false
        // OBS: sjekken gjøres på hele rettighetsperioden. Må endres til stønadsperiode når krav er på plass
        return avslagSykdomForHelePerioden(sykdomsGrunnlag.sykdomsvurderinger, kontekst.rettighetsperiode)
    }

    private fun avslagSykdomForHelePerioden(
        sykdomsvurderinger: List<Sykdomsvurdering>,
        rettighetsperiode: Periode
    ): Boolean {
        val sykdomsvurderingTidslinje = sykdomsvurderinger.somSykdomsvurderingTidslinje()
        return sykdomsvurderinger.isNotEmpty() && sykdomsvurderingTidslinje.all { it.erIkkeOppfylt() }
                && sykdomsvurderingTidslinje.helePerioden() == rettighetsperiode
                && sykdomsvurderingTidslinje.erSammenhengende()
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FatteVedtakSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                trekkKlageService = TrekkKlageService(repositoryProvider),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
                avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
                trukketSøknadService = TrukketSøknadService(repositoryProvider),
                avbrytAktivitetspliktbehandlingService = AvbrytAktivitetspliktbehandlingService(repositoryProvider),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
                klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
                vedtakService = VedtakService(repositoryProvider, gatewayProvider),
                virkningstidspunktService = VirkningstidspunktService(repositoryProvider, gatewayProvider),
                sykdomRepository = repositoryProvider.provide(),
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                unleashGateway = gatewayProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.FATTE_VEDTAK
        }
    }

}