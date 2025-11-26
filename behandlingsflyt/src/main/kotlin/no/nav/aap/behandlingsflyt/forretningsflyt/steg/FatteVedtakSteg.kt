package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeføresFraBeslutter
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime
import java.time.ZoneId

class FatteVedtakSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val trekkKlageService: TrekkKlageService,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val vedtakService: VedtakService,
    private val virkningstidspunktUtleder: VirkningstidspunktUtleder,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        val vedtakBehøverVurdering = vedtakBehøverVurdering(kontekst, avklaringsbehovene)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.FATTE_VEDTAK,
            vedtakBehøverVurdering = { vedtakBehøverVurdering },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert(kontekst, avklaringsbehovene) },
            tilbakestillGrunnlag = {},
            kontekst = kontekst
        )

        if (avklaringsbehovene.skalTilbakeføresEtterTotrinnsVurdering()) {
            return TilbakeføresFraBeslutter
        }

        if (unleashGateway.isEnabled(BehandlingsflytFeature.LagreVedtakIFatteVedtak)) {
            val vedtakstidspunkt = if (vedtakBehøverVurdering)
                avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FATTE_VEDTAK)
                    ?.historikk
                    ?.singleOrNull { it.status == Status.AVSLUTTET }
                    ?.tidsstempel
            else
                LocalDateTime.now(ZoneId.of("Europe/Oslo"))

            if (vedtakstidspunkt != null) {
                vedtakService.lagreVedtak(
                    behandlingId = kontekst.behandlingId,
                    vedtakstidspunkt = vedtakstidspunkt,
                    virkningstidspunkt = virkningstidspunktUtleder.utledVirkningsTidspunkt(kontekst.behandlingId),
                )
            }
        }

        return Fullført
    }

    private fun vedtakBehøverVurdering(
        kontekst: FlytKontekstMedPerioder,
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) ||
            trekkKlageService.klageErTrukket(kontekst.behandlingId)
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
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        val harHattAvklaringsbehovSomHarKrevdTotrinnOgSomIkkeErVurdert =
            avklaringsbehovene.harAvklaringsbehovSomKreverToTrinnMenIkkeErVurdert()

        val erKlage = kontekst.behandlingType == TypeBehandling.Klage
        val erTrukketEllerIngenGrunnlag =
            tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) ||
                    trekkKlageService.klageErTrukket(kontekst.behandlingId)

        return when {
            erTrukketEllerIngenGrunnlag -> true
            erKlage -> true
            harHattAvklaringsbehovSomHarKrevdTotrinnOgSomIkkeErVurdert -> false
            else -> true
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FatteVedtakSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                trekkKlageService = TrekkKlageService(repositoryProvider),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
                klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
                vedtakService = VedtakService(repositoryProvider),
                virkningstidspunktUtleder = VirkningstidspunktUtleder(repositoryProvider),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.FATTE_VEDTAK
        }
    }
}