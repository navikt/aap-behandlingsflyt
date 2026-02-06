package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.meldekort.kontrakt.sak.BehandslingsflytUtfyllingRequest
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0

interface MeldekortGateway: Gateway {
    fun oppdaterMeldeperioder(meldeperioderV0: MeldeperioderV0)
    fun sendTimerArbeidetIPeriode(request: BehandslingsflytUtfyllingRequest)
}