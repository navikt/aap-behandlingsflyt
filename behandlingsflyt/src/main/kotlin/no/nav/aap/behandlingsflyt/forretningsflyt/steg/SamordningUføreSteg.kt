package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class SamordningUføreSteg(
    private val uføreRepository: UføreRepository,
    private val behandlingRepository: BehandlingRepository,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                val uføreGrunnlag = uføreRepository.hentHvisEksisterer(kontekst.behandlingId)
                if (uføreGrunnlag != null) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_UFØRE)
                }
            }

            VurderingType.REVURDERING -> {
                val uføreGrunnlag = uføreRepository.hentHvisEksisterer(kontekst.behandlingId)
                val behandling = behandlingRepository.hent(kontekst.behandlingId)
                val uføreGrunnlagPåForrigeBehandling = behandling.forrigeBehandlingId?.let {
                    uføreRepository.hentHvisEksisterer(it)
                }

                if (uføreGrunnlag?.id != uføreGrunnlagPåForrigeBehandling?.id) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_UFØRE)
                }
            }

            VurderingType.FORLENGELSE -> TODO()
            VurderingType.IKKE_RELEVANT -> {}
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val uføreRepository = repositoryProvider.provide<UføreRepository>()
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            return SamordningUføreSteg(uføreRepository, behandlingRepository)
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_UFØRE
        }
    }
}
