package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.brev.kontrakt.Vedlegg
import java.util.*

data class VarselOmBrevbestillingDto(
    val behandlingsReferanse: BehandlingReferanse,
    val dialogmeldingUuid: UUID,
    val vedlegg: Vedlegg
)