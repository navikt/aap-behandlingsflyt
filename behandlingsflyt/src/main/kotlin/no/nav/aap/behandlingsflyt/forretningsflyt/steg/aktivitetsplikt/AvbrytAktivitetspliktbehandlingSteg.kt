package no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class AvbrytAktivitetspliktbehandlingSteg(
    private val avbrytAktivitetspliktbehandlingRepository: AvbrytAktivitetspliktbehandlingRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val erTilstrekkeligVurdert =
            avbrytAktivitetspliktbehandlingRepository.hentHvisEksisterer(kontekst.behandlingId) != null

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.AVBRYT_AKTIVITETSPLIKTBEHANDING,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert },
            tilbakestillGrunnlag = {},
            kontekst = kontekst
        )
        return Fullført
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return (kontekst.behandlingType == TypeBehandling.Aktivitetsplikt || kontekst.behandlingType == TypeBehandling.Aktivitetsplikt11_9)
                && kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty()
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return AvbrytAktivitetspliktbehandlingSteg(
            avbrytAktivitetspliktbehandlingRepository = repositoryProvider.provide(),
            avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.AVBRYT_AKTIVITETSPLIKTBEHANDLING
        }
    }
}