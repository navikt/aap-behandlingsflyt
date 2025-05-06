package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class SkrivBrevLøser(connection: DBConnection) : AvklaringsbehovsLøser<SkrivBrevLøsning> {

    private val skrivBrevAvklaringsbehovLøser = SkrivBrevAvklaringsbehovLøser(connection)

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivBrevLøsning
    ): LøsningsResultat {
        return skrivBrevAvklaringsbehovLøser.løs(
            kontekst,
            SkrivBrevAvklaringsbehovLøsning(løsning.brevbestillingReferanse, løsning.handling)
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_BREV
    }
}