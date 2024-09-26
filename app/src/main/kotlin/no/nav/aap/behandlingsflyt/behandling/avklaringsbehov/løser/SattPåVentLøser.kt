package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SattPåVentLøsning
import no.nav.aap.komponenter.dbconnect.DBConnection

class SattPåVentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<SattPåVentLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: SattPåVentLøsning): LøsningsResultat {
        return LøsningsResultat("Tatt av vent")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.MANUELT_SATT_PÅ_VENT
    }
}