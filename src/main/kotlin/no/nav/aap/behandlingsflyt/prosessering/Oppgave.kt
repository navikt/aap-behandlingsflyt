package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection

abstract class Oppgave {

    abstract fun utfør(connection: DbConnection, input: OppgaveInput)

    abstract fun type(): String

    override fun toString(): String {
        return type()
    }

}