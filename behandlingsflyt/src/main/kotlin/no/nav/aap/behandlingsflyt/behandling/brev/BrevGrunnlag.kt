package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.BrevdataDto
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.Språk
import java.time.LocalDateTime
import java.util.*

data class BrevGrunnlag(
    val brevGrunnlag: List<Brev>) {
    data class Brev(
        val avklaringsbehovKode: AvklaringsbehovKode,
        val brevbestillingReferanse: UUID,
        val brev: no.nav.aap.brev.kontrakt.Brev?,
        val brevmal: String?,
        val brevdata: BrevdataDto?,
        val opprettet: LocalDateTime,
        val oppdatert: LocalDateTime,
        val brevtype: Brevtype,
        val språk: Språk,
        val status: Status,
        val mottaker: Mottaker,
        val signaturer: List<Signatur>,
        val harTilgangTilÅSendeBrev: Boolean,
    ) {
        data class Mottaker(val navn: String, val ident: String)
    }
}
