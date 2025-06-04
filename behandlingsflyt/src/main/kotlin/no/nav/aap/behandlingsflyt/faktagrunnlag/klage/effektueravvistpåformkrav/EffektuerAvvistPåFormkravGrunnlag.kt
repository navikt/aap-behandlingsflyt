package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import java.time.LocalDate

data class EffektuerAvvistPåFormkravGrunnlag(
    val varsel: ForhåndsvarselKlageFormkrav,
    val vurdering: EffektuerAvvistPåFormkravVurdering?
)

data class ForhåndsvarselKlageFormkrav(
    val datoVarslet: LocalDate? = null,
    val frist: LocalDate? = null,
    val referanse: BrevbestillingReferanse,
)

data class EffektuerAvvistPåFormkravVurdering(
    val skalEndeligAvvises: Boolean
)