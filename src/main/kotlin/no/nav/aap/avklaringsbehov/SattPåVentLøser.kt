package no.nav.aap.avklaringsbehov

import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.kontroll.FlytKontekst

class SattPåVentLøser:AvklaringsbehovsLøser<SattPåVentLøsning>{

    override fun løs(kontekst: FlytKontekst, løsning: SattPåVentLøsning): LøsningsResultat {
        return LøsningsResultat("Tatt av vent")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.MANUELT_SATT_PÅ_VENT
    }
}