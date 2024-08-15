package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class AvklarBarnetilleggLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarBarnetilleggLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarBarnetilleggLøsning): LøsningsResultat {

        return LøsningsResultat(begrunnelse = "TOO BE FIXED")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BARNETILLEGG
    }
}
