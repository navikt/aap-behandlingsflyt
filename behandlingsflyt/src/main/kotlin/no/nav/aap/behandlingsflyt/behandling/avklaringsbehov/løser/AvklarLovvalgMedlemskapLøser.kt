package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class AvklarLovvalgMedlemskapLøser(connection: DBConnection): AvklaringsbehovsLøser<AvklarLovvalgMedlemskapLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarLovvalgMedlemskapLøsning): LøsningsResultat {
        TODO("Not yet implemented")

        return LøsningsResultat("")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP
    }
}