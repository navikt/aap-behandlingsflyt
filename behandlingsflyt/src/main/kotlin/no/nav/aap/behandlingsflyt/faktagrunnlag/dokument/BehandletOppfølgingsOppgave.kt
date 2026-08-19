package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.HvemSkalFølgeOpp
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Oppfølgingsoppgave
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opprinnelse
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate


data class BehandletOppfølgingsOppgave(
    val datoForOppfølging: LocalDate,
    val hvemSkalFølgeOpp: HvemSkalFølgeOpp,
    val hvaSkalFølgesOpp: String,
    val reserverTilBruker: Bruker?,
    val opprettetAv: Bruker?,
    val opprinnelse: Opprinnelse? = null
) {
    companion object {
        fun fraDokument(dokument: Oppfølgingsoppgave): BehandletOppfølgingsOppgave {
            return when (dokument) {
                is OppfølgingsoppgaveV0 -> BehandletOppfølgingsOppgave(
                    datoForOppfølging = dokument.datoForOppfølging,
                    hvemSkalFølgeOpp = dokument.hvemSkalFølgeOpp,
                    hvaSkalFølgesOpp = dokument.hvaSkalFølgesOpp,
                    reserverTilBruker = dokument.reserverTilBruker?.let(::Bruker),
                    opprettetAv = dokument.opprettetAv?.let(::Bruker),
                    opprinnelse = dokument.opprinnelse
                )
            }
        }
    }
}
