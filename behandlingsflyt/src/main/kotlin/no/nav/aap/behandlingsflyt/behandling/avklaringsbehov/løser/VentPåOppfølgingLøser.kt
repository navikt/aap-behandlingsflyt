package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VentPåOppfølgingLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class VentPåOppfølgingLøser(repositoryProvider: RepositoryProvider) : AvklaringsbehovsLøser<VentPåOppfølgingLøsning> {
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: VentPåOppfølgingLøsning
    ): LøsningsResultat {
        return LøsningsResultat("Tatt av vent")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VENT_PÅ_OPPFØLGING
    }
}