package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.HvemSkalFølgeOpp
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Oppfølgingsoppgave
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opprinnelse
import no.nav.aap.behandlingsflyt.pip.IdentPåSak
import java.time.LocalDate


data class BehandletOppfølgingsOppgave(
    val datoForOppfølging: LocalDate,
    val hvemSkalFølgeOpp: HvemSkalFølgeOpp,
    val hvaSkalFølgesOpp: String,
    val reserverTilBruker: String?,
    val opprinnelse: Opprinnelse?
) {
    companion object {
        fun fraDokument(dokument: Oppfølgingsoppgave): BehandletOppfølgingsOppgave {
            return when (dokument) {
                is OppfølgingsoppgaveV0 -> BehandletOppfølgingsOppgave(
                    datoForOppfølging = dokument.datoForOppfølging,
                    hvemSkalFølgeOpp = dokument.hvemSkalFølgeOpp,
                    hvaSkalFølgesOpp = dokument.hvaSkalFølgesOpp,
                    reserverTilBruker = dokument.reserverTilBruker,
                    opprinnelse = dokument.opprinnelse

                )
            }
        }
    }
}
