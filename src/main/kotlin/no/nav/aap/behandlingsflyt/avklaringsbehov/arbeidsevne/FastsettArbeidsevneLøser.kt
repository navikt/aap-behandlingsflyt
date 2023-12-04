package no.nav.aap.behandlingsflyt.avklaringsbehov.arbeidsevne

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class FastsettArbeidsevneLøser(private val connection: DBConnection) : AvklaringsbehovsLøser<FastsettArbeidsevneLøsning> {
    override fun løs(kontekst: FlytKontekst, løsning: FastsettArbeidsevneLøsning): LøsningsResultat {
        TODO("Not yet implemented")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_ARBEIDSEVNE
    }
}