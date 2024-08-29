package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettArbeidsevneLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.komponenter.dbconnect.DBConnection

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
