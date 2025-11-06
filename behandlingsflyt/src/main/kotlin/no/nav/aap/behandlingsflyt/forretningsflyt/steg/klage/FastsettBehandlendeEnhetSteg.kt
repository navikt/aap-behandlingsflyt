package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
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

class FastsettBehandlendeEnhetSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val trekkKlageService: TrekkKlageService,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            definisjon = Definisjon.FASTSETT_BEHANDLENDE_ENHET,
            vedtakBehøverVurdering = { behøverVurdering(kontekst) },
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = { /* TrekkKlageSteg har ansvar for slettingen */ },
            kontekst = kontekst
        )

        return Fullført
    }

    private fun behøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        val behandlingId = kontekst.behandlingId
        val avslått = klageresultatUtleder.utledKlagebehandlingResultat(behandlingId) is Avslått
        val trukket = trekkKlageService.klageErTrukket(behandlingId)
        return !avslått && !trukket
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FastsettBehandlendeEnhetSteg(
                repositoryProvider.provide(),
                KlageresultatUtleder(repositoryProvider),
                TrekkKlageService(repositoryProvider),
                AvklaringsbehovService(repositoryProvider),
            )
        }

        override fun type(): StegType {
            return StegType.BEHANDLENDE_ENHET
        }
    }

}