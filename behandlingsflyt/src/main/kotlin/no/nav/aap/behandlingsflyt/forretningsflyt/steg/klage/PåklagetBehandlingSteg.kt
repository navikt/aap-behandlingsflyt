package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class PåklagetBehandlingSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val trekkKlageService: TrekkKlageService,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehov,
            definisjon = Definisjon.FASTSETT_PÅKLAGET_BEHANDLING,
            vedtakBehøverVurdering = { !trekkKlageService.klageErTrukket(kontekst.behandlingId)},
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = { /* trekkklagesteget håndterer tilbakestilling */ },
            kontekst = kontekst
        )
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return PåklagetBehandlingSteg(
                avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>(),
                trekkKlageService = TrekkKlageService(repositoryProvider),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
            )
        }

        override fun type(): StegType {
            return StegType.PÅKLAGET_BEHANDLING
        }
    }
}