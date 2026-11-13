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

class VurderSøknadUtenlandsoppholdSteg private constructor(
    private val avklaringsbehovService: AvklaringsbehovService,
    private val søknadUtenlandsoppholdRepository: SøknadUtenlandsoppholdRepository,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehovMedGrad(
            definisjon = Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD,
            behov = { AvklaringsbehovService.Behov.PÅKREVD },
            erTilstrekkeligVurdert = {
                søknadUtenlandsoppholdRepository.hentHvisEksisterer(kontekst.behandlingId) != null
            },
            tilbakestillGrunnlag = { /* Behandlingen har ingen tidligere behandling å tilbakestille til */ },
            kontekst = kontekst,
        )
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderSøknadUtenlandsoppholdSteg(
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
                søknadUtenlandsoppholdRepository = repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_SØKNAD_UTENLANDSOPPHOLD
        }
    }
}
