package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettPåklagetBehandlingLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class FastsettPåklagetBehandlingLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FastsettPåklagetBehandlingLøsning>  {
    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettPåklagetBehandlingLøsning): LøsningsResultat {
        // TODO: Legg inn lagring av løsning
        return LøsningsResultat(begrunnelse = "Fortsetter")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_PÅKLAGET_BEHANDLING
    }
}