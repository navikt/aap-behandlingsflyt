package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.lookup.repository.RepositoryProvider

class KlagebehandlingKontorSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlendeEnhetRepository: BehandlendeEnhetRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val behandlendeEnhetVurdering = behandlendeEnhetRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurdering
        requireNotNull(behandlendeEnhetVurdering) {
            "Behandlende kontor skal være satt"
        }
        return if (behandlendeEnhetVurdering.skalBehandlesAvKontor && avklaringsbehov.harIkkeBlittLøst(Definisjon.VURDER_KLAGE_KONTOR)) {
            FantAvklaringsbehov(Definisjon.VURDER_KLAGE_KONTOR)
        } else Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return KlagebehandlingKontorSteg(repositoryProvider.provide(), repositoryProvider.provide())
        }

        override fun type(): StegType {
            return StegType.KLAGEBEHANDLING_KONTOR
        }
    }

    private fun Avklaringsbehovene.harIkkeBlittLøst(definisjon: Definisjon): Boolean {
        return this.alle()
            .filter { it.definisjon == definisjon }
            .none { it.status() == Status.AVSLUTTET }
    }
}