package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbstuff.DbConnection

object TestFlytSteg : FlytSteg {
    override fun konstruer(connection: DbConnection): BehandlingSteg {
        return TestSteg()
    }

    override fun type(): StegType {
        return StegType.AVKLAR_SYKDOM
    }
}

class TestSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        return StegResultat(avklaringsbehov = listOf(Definisjon.AVKLAR_SYKDOM))
    }
}