package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class FullmektigSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val trekkKlageService: TrekkKlageService
): BehandlingSteg {
    override fun utfør(kontekst: no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            avklaringsbehov.avbrytForSteg(type())
            return Fullført
        }

        return if (avklaringsbehov.harIkkeBlittLøst(Definisjon.FASTSETT_FULLMEKTIG)) {
            FantAvklaringsbehov(Definisjon.FASTSETT_FULLMEKTIG)
        } else Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return FullmektigSteg(
                repositoryProvider.provide(),
                TrekkKlageService(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.FULLMEKTIG
        }
    }

    private fun Avklaringsbehovene.harIkkeBlittLøst(definisjon: Definisjon): Boolean {
        return this.alle()
            .filter { it.definisjon == definisjon }
            .none { it.status() == Status.AVSLUTTET }
    }
    
}