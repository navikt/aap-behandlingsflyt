package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.Effektuer11_7Løsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class Effektuer11_7Løser(
    connection: DBConnection,
) : AvklaringsbehovsLøser<Effektuer11_7Løsning> {
    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: Effektuer11_7Løsning): LøsningsResultat {
        return LøsningsResultat(begrunnelse = løsning.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.EFFEKTUER_11_7
    }
}