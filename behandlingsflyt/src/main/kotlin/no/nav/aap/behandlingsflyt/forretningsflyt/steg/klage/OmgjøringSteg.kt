package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Omgjøres
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

class OmgjøringSteg private constructor(
    private val klageresultatUtleder: KlageresultatUtleder,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val resultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        requireNotNull(resultat) {
            "Klagebehandlingresultat er skal være satt innen dette steget"
        }

        return when (resultat) {
            is Omgjøres, is DelvisOmgjøres -> {
                return if (avklaringsbehovene.harIkkeBlittLøst(Definisjon.OPPRETT_REVURDERING_VED_OMGJØRING)) {
                    FantAvklaringsbehov(Definisjon.OPPRETT_REVURDERING_VED_OMGJØRING)
                } else Fullført
            }

            else -> Fullført
        }
    }

    private fun Avklaringsbehovene.harIkkeBlittLøst(definisjon: Definisjon): Boolean {
        return this.alle()
            .filter { it.definisjon == definisjon }
            .none { it.status() == Status.AVSLUTTET }
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return OmgjøringSteg(
                KlageresultatUtleder(repositoryProvider),
                repositoryProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.OMGJØRING
        }
    }
}