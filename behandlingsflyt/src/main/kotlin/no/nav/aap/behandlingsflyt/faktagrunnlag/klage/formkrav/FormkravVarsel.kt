package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import java.time.LocalDate

data class FormkravVarsel(
    val varselId: BrevbestillingReferanse,
    val sendtDato: LocalDate? = null,
    val svarfrist: LocalDate? = null
)