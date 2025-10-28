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
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class FullmektigSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val trekkKlageService: TrekkKlageService,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {
    override fun utfør(kontekst: no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene,
            definisjon = Definisjon.FASTSETT_FULLMEKTIG,
            vedtakBehøverVurdering = { !trekkKlageService.klageErTrukket(kontekst.behandlingId) },
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = { /* TrekkKlageSteg har ansvar for slettingen */ },
            kontekst = kontekst
        )

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FullmektigSteg(
                repositoryProvider.provide(),
                TrekkKlageService(repositoryProvider),
                AvklaringsbehovService(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.FULLMEKTIG
        }
    }

}