package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VentPåOppfølgingNyLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon

class VentPåOppfølgingNyLøser : AvklaringsbehovsLøser<VentPåOppfølgingNyLøsning> {
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: VentPåOppfølgingNyLøsning
    ): LøsningsResultat {
        return LøsningsResultat("Tatt av vent")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VENT_PÅ_OPPFØLGING_NY
    }
}