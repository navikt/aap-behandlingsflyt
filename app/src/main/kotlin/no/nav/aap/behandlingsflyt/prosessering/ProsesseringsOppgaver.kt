package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.motor.Oppgave

object ProsesseringsOppgaver {

    fun alle() :List<Oppgave> {
        // Legger her alle oppgavene som sakl utføres i systemet
        return listOf(ProsesserBehandlingOppgave)
    }
}