package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LegeerklæringBestillingLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class LegeerklæringBestillingLøser(val connection: DBConnection) :
    AvklaringsbehovsLøser<LegeerklæringBestillingLøsning> {
    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: LegeerklæringBestillingLøsning): LøsningsResultat {
        return LøsningsResultat("Tatt av vent (ventet på bestilling av legeerklæring)")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.BESTILL_LEGEERKLÆRING
    }
}