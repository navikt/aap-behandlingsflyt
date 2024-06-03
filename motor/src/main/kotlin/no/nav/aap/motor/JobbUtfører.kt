package no.nav.aap.motor

interface JobbUtfører {

    fun utfør(input: JobbInput)

    /**
     * Antall ganger oppgaven prøves før den settes til feilet
     */
    fun retries(): Int {
        return 3
    }

}
