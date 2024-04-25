package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.FastsettArbeidsevneLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository

class FastsettArbeidsevneLøser(connection: DBConnection) :
    AvklaringsbehovsLøser<FastsettArbeidsevneLøsning> {

    private val arbeidsevneRepository = ArbeidsevneRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettArbeidsevneLøsning): LøsningsResultat {
        arbeidsevneRepository.lagre(kontekst.kontekst.behandlingId, løsning.arbeidsevne)

        return LøsningsResultat(begrunnelse = løsning.arbeidsevne.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_ARBEIDSEVNE
    }
}
