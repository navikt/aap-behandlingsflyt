package no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class ForeslåVedtakLøser(val connection: DBConnection) : AvklaringsbehovsLøser<ForeslåVedtakLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: ForeslåVedtakLøsning): LøsningsResultat {
        // DO NOTHING 4 Now
        return LøsningsResultat(løsning.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FORESLÅ_VEDTAK
    }
}
