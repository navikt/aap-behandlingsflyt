package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettArbeidsevneLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate.ArbeidsevnevurderingDto
import no.nav.aap.komponenter.dbconnect.DBConnection

class FastsettArbeidsevneLøser(connection: DBConnection) :
    AvklaringsbehovsLøser<FastsettArbeidsevneLøsning> {

    private val arbeidsevneRepository = ArbeidsevneRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettArbeidsevneLøsning): LøsningsResultat {
        val arbeidsevnevurderinger = løsning.arbeidsevneVurderinger.map(ArbeidsevnevurderingDto::toArbeidsevnevurdering)
        val eksisterendeArbeidsevnevurderinger =
            arbeidsevneRepository.hentHvisEksisterer(kontekst.behandlingId())?.vurderinger.orEmpty()

        arbeidsevneRepository.lagre(kontekst.behandlingId(), eksisterendeArbeidsevnevurderinger + arbeidsevnevurderinger)

        //return LøsningsResultat(begrunnelse = løsning.arbeidsevneVurderinger.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_ARBEIDSEVNE
    }
}
