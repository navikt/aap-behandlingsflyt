package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

object TestFlytSteg : FlytSteg {
    override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
        return TestSteg(repositoryProvider.provide())
    }

    override fun type(): StegType {
        return StegType.AVKLAR_SYKDOM
    }
}

class TestSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId).leggTil(
            Definisjon.AVKLAR_SYKDOM, funnetISteg = StegType.AVKLAR_SYKDOM,
            perioderSomIkkeErTilstrekkeligVurdert = null,
            perioderVedtaketBehøverVurdering = null,
        )
        return Fullført
    }
}
