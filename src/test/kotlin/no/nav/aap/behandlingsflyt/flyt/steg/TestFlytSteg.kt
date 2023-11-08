package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

object TestFlytSteg : FlytSteg {
    override fun konstruer(connection: DBConnection): BehandlingSteg {
        return TestSteg()
    }

    override fun type(): StegType {
        return StegType.AVKLAR_SYKDOM
    }
}

class TestSteg : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekst): StegResultat {
        return StegResultat(avklaringsbehov = listOf(Definisjon.AVKLAR_SYKDOM))
    }
}
