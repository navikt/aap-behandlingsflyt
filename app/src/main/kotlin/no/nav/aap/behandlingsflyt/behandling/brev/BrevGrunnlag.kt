package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import java.time.LocalDateTime
import java.util.UUID

data class BrevGrunnlag(
    val brevbestillingReferanse: UUID,
    val brev: Brev?,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
    val brevtype: Brevtype,
    val språk: Språk,
    val status: Status,
    val mottaker: Mottaker,
)

data class Mottaker(val navn: String, val ident: String)