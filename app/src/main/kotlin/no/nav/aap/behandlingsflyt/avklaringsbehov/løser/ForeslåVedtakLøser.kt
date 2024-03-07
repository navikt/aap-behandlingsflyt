package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst

class ForeslåVedtakLøser(val connection: DBConnection) : AvklaringsbehovsLøser<ForeslåVedtakLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: ForeslåVedtakLøsning): LøsningsResultat {
        // DO NOTHING 4 Now
        return LøsningsResultat(løsning.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FORESLÅ_VEDTAK
    }
}
