package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0

interface MeldekortGateway: Gateway {
    fun oppdaterMeldeperioder(meldeperioderV0: MeldeperioderV0)
}