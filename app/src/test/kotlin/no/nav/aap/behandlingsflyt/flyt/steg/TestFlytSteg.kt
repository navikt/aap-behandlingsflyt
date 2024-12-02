package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder

object TestFlytSteg : FlytSteg {
    override fun konstruer(connection: DBConnection): BehandlingSteg {
        return TestSteg()
    }

    override fun type(): StegType {
        return StegType.AVKLAR_SYKDOM
    }
}

class TestSteg : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return FantAvklaringsbehov(Definisjon.AVKLAR_SYKDOM)
    }
}
