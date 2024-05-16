package no.nav.aap.behandlingsflyt.avklaringsbehov

class BehandlingUnderProsesseringException() : RuntimeException("Behandlingen har prosesseringsoppgaver som venter eller har feilet. Vent til disse er ferdig prosesserte")
