package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Omgjøres
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.lookup.repository.RepositoryProvider

class OmgjøringSteg private constructor(
    private val klageresultatUtleder: KlageresultatUtleder
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val resultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
        requireNotNull(resultat) {
            "Klagebehandlingresultat er skal være satt innen dette steget"
        }

        return when (resultat) {
            is Omgjøres, is DelvisOmgjøres -> {
                FantAvklaringsbehov(Definisjon.OPPRETT_REVURDERING_VED_OMGJØRING)
            } else -> Fullført
        }
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return OmgjøringSteg(KlageresultatUtleder(repositoryProvider))
        }

        override fun type(): StegType {
            return StegType.OMGJØRING
        }
    }
}