package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderOppholdskravSteg private constructor(
    private val oppholdskravGrunnlagRepository: OppholdskravGrunnlagRepository
) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider) : this(
        oppholdskravGrunnlagRepository = repositoryProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (oppholdskravGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId) != null) {
            return Fullført
        }
        if (kontekst.erFørstegangsbehandling() || kontekst.erRevurderingMedVurderingsbehov(Vurderingsbehov.OPPHOLDSKRAV)) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_OPPHOLDSKRAV)
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderOppholdskravSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_OPPHOLDSKRAV
        }
    }
}
