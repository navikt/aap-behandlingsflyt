package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prosessering.MeldekortGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.meldekort.kontrakt.sak.BehandslingsflytUtfyllingRequest
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0

class FakeMeldekortGateway: MeldekortGateway {
    override fun oppdaterMeldeperioder(meldeperioderV0: MeldeperioderV0) {
        TODO("Not yet implemented")
    }

    override fun sendTimerArbeidetIPeriode(arbeidstimerRequest: BehandslingsflytUtfyllingRequest) {
        TODO("Not yet implemented")
    }

    override fun oppdaterIdenter(
        saksnummer: Saksnummer,
        identer: List<Ident>
    ) {
        TODO("Not yet implemented")
    }

    companion object : Factory<MeldekortGateway> {
        override fun konstruer(): MeldekortGateway = FakeMeldekortGateway()
    }

}