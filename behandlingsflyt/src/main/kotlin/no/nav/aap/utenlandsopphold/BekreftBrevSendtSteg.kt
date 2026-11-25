package no.nav.aap.utenlandsopphold

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class BekreftBrevSendtSteg private constructor(
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehovMedGrad(
            definisjon = Definisjon.BEKREFT_BREV_SENDT_MANUELT,
            behov = { AvklaringsbehovService.Behov.PÅKREVD },
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = { /* Bekreftelsen lagres kun på avklaringsbehovet */ },
            kontekst = kontekst,
        )
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return BekreftBrevSendtSteg(
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
            )
        }

        override fun type(): StegType {
            return StegType.BEKREFT_BREV_SENDT
        }
    }
}
