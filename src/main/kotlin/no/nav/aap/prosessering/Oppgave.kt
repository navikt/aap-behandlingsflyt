package no.nav.aap.prosessering

abstract class Oppgave {

    abstract fun utfør(input: OppgaveInput)

    abstract fun type(): String

    override fun toString(): String {
        return type()
    }

}