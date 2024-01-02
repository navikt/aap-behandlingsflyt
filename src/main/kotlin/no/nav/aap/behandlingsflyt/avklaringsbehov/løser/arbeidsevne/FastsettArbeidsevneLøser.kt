package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.arbeidsevne

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class FastsettArbeidsevneLøser(connection: DBConnection) :
    AvklaringsbehovsLøser<FastsettArbeidsevneLøsning> {

    private val arbeidsevneRepository = ArbeidsevneRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: FastsettArbeidsevneLøsning): LøsningsResultat {
        arbeidsevneRepository.lagre(kontekst.behandlingId, løsning.arbeidsevne)

        return LøsningsResultat(begrunnelse = løsning.arbeidsevne.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_ARBEIDSEVNE
    }
}
