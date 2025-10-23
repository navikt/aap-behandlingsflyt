package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayRepository
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class KlagebehandlingNaySteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlendeEnhetRepository: BehandlendeEnhetRepository,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val trekkKlageService: TrekkKlageService,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val klagebehandlingNayRepository: KlagebehandlingNayRepository,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val resultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
        val erKlageResultatAvslått = resultat is Avslått

        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val vurderingHosNay = klagebehandlingNayRepository.hentHvisEksisterer(kontekst.behandlingId) != null

        val klageErTrukket = trekkKlageService.klageErTrukket(kontekst.behandlingId)


        val behandlendeEnhetVurdering = behandlendeEnhetRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurdering
        //kan være null
        val skalBehandlesAvNay = if (behandlendeEnhetVurdering?.skalBehandlesAvNay == true) true else false


        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.VURDER_KLAGE_NAY,
            avklaringsbehovene = avklaringsbehov,
            kontekst = kontekst,
            vedtakBehøverVurdering = {
                if (klageErTrukket || erKlageResultatAvslått || !skalBehandlesAvNay) false else true
            },
            erTilstrekkeligVurdert = {
                vurderingHosNay
            },
            tilbakestillGrunnlag = {}

        )
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return KlagebehandlingNaySteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                KlageresultatUtleder(repositoryProvider),
                TrekkKlageService(repositoryProvider),
                AvklaringsbehovService(repositoryProvider),
                repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.KLAGEBEHANDLING_NAY
        }
    }


}