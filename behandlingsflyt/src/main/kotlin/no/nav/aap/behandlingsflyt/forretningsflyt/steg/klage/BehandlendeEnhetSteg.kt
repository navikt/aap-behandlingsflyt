package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
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

class BehandlendeEnhetSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return if (avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                .harIkkeBlittLøst(Definisjon.FASTSETT_BEHANDLENDE_ENHET)
        ) {
            FantAvklaringsbehov(Definisjon.FASTSETT_BEHANDLENDE_ENHET)
        } else {
            Fullført
        }
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return BehandlendeEnhetSteg(repositoryProvider.provide())
        }

        override fun type(): StegType {
            return StegType.BEHANDLENDE_ENHET
        }
    }

    private fun Avklaringsbehovene.harIkkeBlittLøst(definisjon: Definisjon): Boolean {
        return this.alle()
            .filter { it.definisjon == definisjon }
            .none { it.status() == Status.AVSLUTTET }
    }
}