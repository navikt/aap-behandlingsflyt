package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
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
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class FatteVedtakSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val trekkKlageService: TrekkKlageService,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val klageresultatUtleder: KlageresultatUtleder,

    ) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) || trekkKlageService.klageErTrukket(
                kontekst.behandlingId
            )
        ) {
            avklaringsbehovService.oppdaterAvklaringsbehov(
                avklaringsbehovene = avklaringsbehovene,
                definisjon = Definisjon.FATTE_VEDTAK,
                vedtakBehøverVurdering = { false },
                erTilstrekkeligVurdert = { true },
                tilbakestillGrunnlag = {},
                kontekst
            )
            return Fullført
        }

        if (kontekst.behandlingType == TypeBehandling.Klage) {
            val klageresultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
            if (klageresultat is Opprettholdes) {
                avklaringsbehovService.oppdaterAvklaringsbehov(
                    avklaringsbehovene = avklaringsbehovene,
                    definisjon = Definisjon.FATTE_VEDTAK,
                    vedtakBehøverVurdering = { false },
                    erTilstrekkeligVurdert = { true },
                    tilbakestillGrunnlag = {},
                    kontekst
                )
                return Fullført
            }
        } else {
            avklaringsbehovService.oppdaterAvklaringsbehov(
                avklaringsbehovene = avklaringsbehovene,
                definisjon = Definisjon.FATTE_VEDTAK,
                vedtakBehøverVurdering = { false },
                erTilstrekkeligVurdert = { true },
                tilbakestillGrunnlag = {},
                kontekst
            )
        }


        if (avklaringsbehovene.skalTilbakeføresEtterTotrinnsVurdering()) {
            return TilbakeføresFraBeslutter
        }
        if (avklaringsbehovene.harHattAvklaringsbehovSomHarKrevdToTrinn()) {
            avklaringsbehovService.oppdaterAvklaringsbehov(
                avklaringsbehovene = avklaringsbehovene,
                definisjon = Definisjon.FATTE_VEDTAK,
                vedtakBehøverVurdering = { true },
                erTilstrekkeligVurdert = { true },
                tilbakestillGrunnlag = {},
                kontekst
            )
        }

        return Fullført
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
                klageresultatUtleder = KlageresultatUtleder(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.FATTE_VEDTAK
        }
    }
}