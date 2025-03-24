package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningAndreStatligeYtelserLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class AvklarSamordningAndreStatligeYtelserLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarSamordningAndreStatligeYtelserLøsning> {


    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningAndreStatligeYtelserLøsning
    ): LøsningsResultat {
        return LøsningsResultat("Vurdert samordning andre statlige ytelser")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SAMORDNING_ANDRE_STATLIGE_YTELSER
    }
}