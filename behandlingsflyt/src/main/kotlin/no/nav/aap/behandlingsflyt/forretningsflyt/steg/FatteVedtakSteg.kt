package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeføresFraBeslutter
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class FatteVedtakSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val trekkKlageService: TrekkKlageService,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val unleashGateway: UnleashGateway,

    ) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isEnabled(BehandlingsflytFeature.FatteVedtakAvklaringsbehovService)) {
            return utførNy(kontekst)
        }
        return utførGammel(kontekst)
    }

    fun utførNy(kontekst: FlytKontekstMedPerioder): StegResultat {


        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        val skalTilbakeføres = avklaringsbehovene.skalTilbakeføresEtterTotrinnsVurdering()
        val harHattAvklaringsbehovSomHarKrevdTotrinnOgSomIkkeErVurdert = avklaringsbehovene.harAvklaringsbehovSomKreverToTrinnMenIkkeErVurdert()

        val erKlage = kontekst.behandlingType == TypeBehandling.Klage
        val erTrukketEllerIngenGrunnlag =
            tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) ||
                    trekkKlageService.klageErTrukket(kontekst.behandlingId)

        val vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst, avklaringsbehovene) }

        val erTilstrekkeligVurdert = when {
            erTrukketEllerIngenGrunnlag -> true
            erKlage -> true
            harHattAvklaringsbehovSomHarKrevdTotrinnOgSomIkkeErVurdert -> false
            else -> true
        }

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.FATTE_VEDTAK,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert },
            tilbakestillGrunnlag = {},
            kontekst = kontekst
        )

        if (skalTilbakeføres) return TilbakeføresFraBeslutter

        return Fullført
    }

    fun utførGammel(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) || trekkKlageService.klageErTrukket(
                kontekst.behandlingId
            )
        ) {
            avklaringsbehov.avbrytForSteg(type())
            return Fullført
        }

        if (kontekst.behandlingType == TypeBehandling.Klage) {
            val klageresultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
            if (klageresultat is Opprettholdes) {
                avklaringsbehov.avbrytForSteg(type())
                return Fullført
            }
        }

        if (avklaringsbehov.skalTilbakeføresEtterTotrinnsVurdering()) {
            return TilbakeføresFraBeslutter
        }
        if (avklaringsbehov.harHattAvklaringsbehovSomHarKrevdToTrinn()) {
            return FantAvklaringsbehov(Definisjon.FATTE_VEDTAK)
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

        return  avklaringsbehovene.harAvklaringsbehovSomKreverToTrinn()
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FatteVedtakSteg(
                repositoryProvider.provide(),
                TrekkKlageService(repositoryProvider),
                AvklaringsbehovService(repositoryProvider),
                TidligereVurderingerImpl(repositoryProvider),
                klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.FATTE_VEDTAK
        }
    }
}