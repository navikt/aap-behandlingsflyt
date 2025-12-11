package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class TrekkKlageSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val trekkKlageRepository: TrekkKlageRepository,
    private val repositoryProvider: RepositoryProvider,
): BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)
    private val avklaringsbehovService = AvklaringsbehovService(repositoryProvider)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val trekkKlageGrunnlag = trekkKlageRepository.hentTrekkKlageGrunnlag(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehov,
            definisjon = Definisjon.VURDER_TREKK_AV_KLAGE,
            vedtakBehøverVurdering = { Vurderingsbehov.KLAGE_TRUKKET in kontekst.vurderingsbehovRelevanteForSteg },
            erTilstrekkeligVurdert = { trekkKlageGrunnlag != null },
            tilbakestillGrunnlag = {},
            kontekst
        )

        if (trekkKlageGrunnlag != null && trekkKlageGrunnlag.vurdering.skalTrekkes) {
            slettVurderingerOgRegisterdata(kontekst.behandlingId)
        }

        return Fullført
    }

    private fun slettVurderingerOgRegisterdata(behandlingId: BehandlingId) {
        log.info("sletter vurderinger og registerdata i alle repositories for klage {}", behandlingId)
        repositoryProvider.provideAlle().forEach { repository ->
            if (repository is no.nav.aap.lookup.repository.Repository) {
                repository.slett(behandlingId)
            }
        }
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return TrekkKlageSteg (
                avklaringsbehovRepository = repositoryProvider.provide(),
                trekkKlageRepository = repositoryProvider.provide(),
                repositoryProvider = repositoryProvider
            )
        }

        override fun type(): StegType {
            return StegType.TREKK_KLAGE
        }
    }
}
