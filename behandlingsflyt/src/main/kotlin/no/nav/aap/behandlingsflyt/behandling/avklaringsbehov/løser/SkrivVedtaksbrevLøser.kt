package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivVedtaksbrevLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class SkrivVedtaksbrevLøser(connection: DBConnection) : AvklaringsbehovsLøser<SkrivVedtaksbrevLøsning> {

    private val skrivBrevAvklaringsbehovLøser = SkrivBrevAvklaringsbehovLøser(connection)
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivVedtaksbrevLøsning
    ): LøsningsResultat {
        return skrivBrevAvklaringsbehovLøser.løs(
            kontekst,
            SkrivBrevAvklaringsbehovLøsning(løsning.brevbestillingReferanse, løsning.handling)
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_VEDTAKSBREV
    }
}
