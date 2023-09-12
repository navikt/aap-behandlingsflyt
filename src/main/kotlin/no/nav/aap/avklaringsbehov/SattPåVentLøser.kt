package no.nav.aap.avklaringsbehov

import no.nav.aap.avklaringsbehov.sykdom.AvklarSykdomLøsning
import no.nav.aap.avklaringsbehov.sykdom.SykdomsFaktagrunnlag
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Vilkårsperiode
import no.nav.aap.domene.behandling.Vilkårstype
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