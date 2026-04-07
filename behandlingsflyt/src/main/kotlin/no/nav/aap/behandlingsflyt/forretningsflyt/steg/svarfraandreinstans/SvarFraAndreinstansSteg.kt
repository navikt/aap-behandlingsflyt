package no.nav.aap.behandlingsflyt.forretningsflyt.steg.svarfraandreinstans

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

class SvarFraAndreinstansSteg private constructor(
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.HÅNDTER_SVAR_FRA_ANDREINSTANS,
            vedtakBehøverVurdering = { true },
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = {},
            kontekst = kontekst,
        )

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return SvarFraAndreinstansSteg(
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
            )
        }

        override fun type(): StegType {
            return StegType.SVAR_FRA_ANDREINSTANS
        }
    }

}