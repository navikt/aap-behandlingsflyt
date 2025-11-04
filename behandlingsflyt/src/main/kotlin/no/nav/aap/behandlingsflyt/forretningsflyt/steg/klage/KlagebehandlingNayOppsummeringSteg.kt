package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class KlagebehandlingNayOppsummeringSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlendeEnhetRepository: BehandlendeEnhetRepository,
    private val trekkKlageService: TrekkKlageService,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.BEKREFT_TOTALVURDERING_KLAGE,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = {},
            kontekst
        )

        return Fullført
    }

    private fun vedtakBehøverVurdering(
        kontekst: FlytKontekstMedPerioder,
    ): Boolean {
        if (girAvslag(kontekst) || trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            return false
        }

        val vurdering = behandlendeEnhetRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurdering
            ?: throw IllegalStateException("Behandlende enhet skal være satt")

        return vurdering.skalBehandlesAvBådeNavKontorOgNay()
    }

    private fun girAvslag(kontekst: FlytKontekstMedPerioder): Boolean {
        val foreløpigKlageresultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
        return foreløpigKlageresultat is Avslått
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return KlagebehandlingNayOppsummeringSteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                TrekkKlageService(repositoryProvider),
                KlageresultatUtleder(repositoryProvider),
                AvklaringsbehovService(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.KLAGEBEHANDLING_OPPSUMMERING
        }
    }

}