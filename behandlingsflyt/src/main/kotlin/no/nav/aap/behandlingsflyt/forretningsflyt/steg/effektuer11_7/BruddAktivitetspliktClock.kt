package no.nav.aap.behandlingsflyt.forretningsflyt.steg.effektuer11_7

import java.time.LocalDate

interface BruddAktivitetspliktClock {
    fun now(): LocalDate
}