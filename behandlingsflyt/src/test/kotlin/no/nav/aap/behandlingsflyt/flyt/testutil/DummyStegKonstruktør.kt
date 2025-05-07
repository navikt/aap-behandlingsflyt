package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.flyt.GeneriskTestSteg
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegKonstruktør

class DummyStegKonstruktør : StegKonstruktør {
    override fun konstruer(steg: FlytSteg): BehandlingSteg {
        return GeneriskTestSteg()
    }
}